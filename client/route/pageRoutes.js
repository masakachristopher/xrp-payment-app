const express = require('express');
const router = express.Router();

router.get('/', (_, res) => {
    res.sendFile('pay.html', { root: 'views' });
});

router.get('/pay', (_, res) => {
    res.sendFile('pay.html', { root: 'views' });
});

router.get('/payment/success', (req, res) => {
    req.session?.destroy(() => { });
    res.sendFile('success.html', { root: 'views' });
});

router.get('/payment/failure', (req, res) => {
    req.session?.destroy(() => { });
    res.sendFile('failure.html', { root: 'views' });
});

module.exports = router;
