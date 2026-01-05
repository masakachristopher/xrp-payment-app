const express = require('express');
const axios = require('axios');
const Paths = require('./constants/paths');

const app = express();
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

app.use(require('express-session')({
  secret: 'xaman-secret',
  resave: false,
  saveUninitialized: true
}));


// Store in memory
const pendingPayments = new Map();

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

  if (payment.status === 'WAITING_USER') {
    // Todo: if need, query details from xaman Api to confirm signing before marking as signed
    payment.status = 'USER_SIGNED';
    console.log('User payment signed, marking completed');

    // Send backend callback
    try {
      const webhookPayload = {
        paymentUuid: payment.userUuid,
        status: 'SIGNED'
      };

      const response = await axios.post(Paths.PAYMENT_CALLBACK, webhookPayload);

      const {
        status,
        feeTxId,
        userTxId,
        message
      } = response.data;

      console.log('Spring Boot callback called successfully');

      // Cleanup state
      pendingPayments.delete(payment.userUuid);

      if (response.status === 200 && status === 'COMPLETED') {
        return res.redirect('/payment/success');
      }

    // Redirect to failure page
    return res.redirect('/payment/failure');
    } catch (err) {
      console.error('Failed to call Spring Boot callback:', err.message);
      // optionally retry or log for later
    }
    
    
  }

  // Fallback
  console.log('Payment is already completed or unknown state');
  return res.redirect('/pay');
});

app.get('/', (req, res) => {
    res.sendFile('pay.html', { root: 'views' });
});

app.post('/payment/initiate', async (req, res) => {
  const { userName, senderAddress, destinationAddress, amount } = req.body;

  const response = await axios.post(
    Paths.PAYMENT_INITIATE,
    { userName, senderAddress, destinationAddress, amount },
    { headers: { RequestId: 'req-' + Date.now() } }
  );

  const {
    userUuid,
    userRedirectUrl
  } = response.data;


  // Save payment state
  pendingPayments.set(userUuid, {
    status: 'WAITING_USER',
    userUuid,
    userRedirectUrl
  });
 
  // Redirect to payment signing
  res.redirect(userRedirectUrl);

});

app.get('/pay', (req, res) => {
  res.sendFile('pay.html', { root: 'views' });
});

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

// app.post('/xaman/webhook', (req, res) => {
//   const { payload_uuidv4, signed } = req.body;

//   if (signed && pendingPayments.has(payload_uuidv4)) {
//     pendingPayments.get(payload_uuidv4).status = 'SIGNED';
//   }

//   res.sendStatus(200);
// });


const PORT = 3000;
app.listen(PORT, () => {
  console.log(`Listening on port ${PORT}`);
});

