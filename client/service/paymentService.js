const axios = require('axios');
const Paths = require('../constants/paths');

exports.initiate = async (payload) => {
    const response = await axios.post(
        Paths.PAYMENT_INITIATE,
        payload,
        { headers: { RequestId: `req-${Date.now()}` } }
    );

    return response.data.userRedirectUrl;
};

exports.verify = async (paymentUuid) => {
    const response = await axios.post(
        Paths.PAYMENT_CALLBACK,
        { paymentUuid }
    );

    return response.data.status;
};
