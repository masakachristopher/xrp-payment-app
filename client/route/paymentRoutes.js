const express = require('express');
const paymentService = require('../service/paymentService');

const router = express.Router();

router.post('/initiate', async (req, res, next) => {
    try {
        const redirectUrl = await paymentService.initiate(req.body);
        return res.redirect(redirectUrl);
    } catch (err) {
        next(err);
    }
});

router.get('/xaman/callback', async (req, res) => {
    const { paymentUuid } = req.query;

    if (!paymentUuid) {
        return res.redirect('/payment/failure');
    }

    try {
        const status = await paymentService.verify(paymentUuid);

        return status === 'COMPLETED'
            ? res.redirect('/payment/success')
            : res.redirect('/payment/failure');

    } catch (err) {
        console.error('Verification failed:', err.message);
        return res.redirect('/payment/failure');
    }
});

module.exports = router;
