const express = require('express');
const axios = require('axios');
const Paths = require('./constants/paths');

const app = express();
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(express.static('public'));

app.use(require('express-session')({
  secret: 'xaman-secret',
  resave: false,
  saveUninitialized: true
}));


const pendingPayments = new Map(); // in-memory (use Redis in prod)

const getLastPendingPayment = () => {
  // Return last payment not completed
  const pending = Array.from(pendingPayments.values()).filter(
    p => p.status !== 'COMPLETED'
  );
  return pending[pending.length - 1];
};

app.get('/xaman/callback', async (req, res) => {
  const payment = getLastPendingPayment();
  console.log('Last pending payment:', payment);

  if (!payment) {
    return res.send('No pending payment found');
  }

  // Step 1 → Fee was signed
  if (payment.status === 'WAITING_FEE') {
    payment.status = 'FEE_SIGNED';
    console.log('➡ Fee signed, redirecting to user payment');
    return res.redirect(payment.userRedirectUrl);
  }

  // Step 2 → User payment was signed
  if (payment.status === 'WAITING_USER' || payment.status === 'FEE_SIGNED') {
    payment.status = 'USER_SIGNED';
    console.log('➡ User payment signed, marking completed');

    // Call backend webhook asynchronously
    try {
      const webhookPayload = {
        paymentUuids: [payment.userUuid, payment.feeUuid],
        status: 'SIGNED'
      };

      const response = await axios.post(Paths.PAYMENT_CALLBACK_BATCH, webhookPayload);
      const {
        status,
        feeTxId,
        userTxId,
        message
      } = response.data;

      console.log('Spring Boot webhook called successfully');

      // Cleanup state
      pendingPayments.delete(payment.feeUuid);

      if (response.status === 200 && status === 'COMPLETED') {
        return res.redirect('/payment/success');
      }

    // Redirect to beautiful failure page
    return res.redirect('/payment/failure');
    } catch (err) {
      console.error('Failed to call Spring Boot webhook:', err.message);
      // optionally retry or log for later
    }
    
    
  }

  // fallback
  // res.send('Payment is already completed or unknown state');
  console.log('Payment is already completed or unknown state');
  return res.redirect('/pay');
});

app.get('/', (req, res) => {
    // res.render('pay');
    // return res.redirect('/pay');
    res.sendFile('pay.html', { root: 'views' });
});

app.post('/payment/initiate', async (req, res) => {
  const { userId, destinationAddress, amount } = req.body;

  const response = await axios.post(
    Paths.PAYMENT_INITIATE_BATCH,
    { userId, destinationAddress, amount },
    { headers: { RequestId: 'req-' + Date.now() } }
  );

  const {
    feeUuid,
    feeRedirectUrl,
    userUuid,
    userRedirectUrl
  } = response.data;


  // SAVE IN SESSION
  req.session.paymentId = feeUuid;

  // SAVE STATE
  pendingPayments.set(feeUuid, {
    status: 'WAITING_FEE',
    userUuid,
    feeUuid,
    userRedirectUrl
  });
 
  // Redirect to fee signing
  res.redirect(feeRedirectUrl);

});

app.get('/payment/status', (req, res) => {
  const paymentId = req.session.paymentId;
  const payment = pendingPayments.get(paymentId);

  if (!payment) {
    return res.json({ next: 'WAIT' });
  }

  if (payment.status === 'WAITING_FEE') {
    return res.json({ next: 'FEE_SIGN', message: 'Fee signing pending' });
  }

  if (payment.status === 'FEE_SIGNED') {
    return res.json({
      next: 'USER_SIGN',
      userRedirectUrl: payment.userRedirectUrl
    });
  }

  if (payment.status === 'USER_SIGNED') {
    return res.json({ next: 'DONE' });
  }

  res.json({ next: 'WAIT' });
});

app.get('/pay', (req, res) => {
  res.sendFile('pay.html', { root: 'views' });
});

app.get('/waiting/:feeUuid', (req, res) => {
  res.sendFile('waiting.html', { root: 'views' });
});

// app.get('/payment/continue/:feeUuid', (req, res) => {
//   const data = pendingPayments.get(req.params.feeUuid);
//   if (!data) return res.status(404).send('Not found');

//   res.redirect(data.userRedirectUrl);
// });

app.get('/payment/success', (req, res) => {
  if (req.session) {
    req.session.destroy(() => {});
  }
  res.sendFile('success.html', { root: 'views' });
});

app.get('/payment/failure', (req, res) => {
  if (req.session) {
    req.session.destroy(() => {});
  }
  res.sendFile('failure.html', { root: 'views' });
});

app.post('/xaman/webhook', (req, res) => {
  const { payload_uuidv4, signed } = req.body;

  if (signed && pendingPayments.has(payload_uuidv4)) {
    pendingPayments.get(payload_uuidv4).status = 'SIGNED';
  }

  res.sendStatus(200);
});


const PORT = 3000;
app.listen(PORT, () => {
  console.log(`🚀 Listening on port ${PORT}`);
});

