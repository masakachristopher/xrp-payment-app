class Paths { 
    // static BASE = '/api/v1';
    static BACKEND_BASE_V2 = 'http://localhost:8080/api/v2';
    static PAYMENT_INITIATE = `${Paths.BACKEND_BASE_V2}/payments/initiate`;
    static PAYMENT_INITIATE_BATCH = `${Paths.BACKEND_BASE_V2}/payments/initiate/batch`;

    static PAYMENT_STATUS = `${Paths.BACKEND_BASE_V2}/payments/status`;
    static PAYMENT_CALLBACK = `${Paths.BACKEND_BASE_V2}/payments/callback`;
    static PAYMENT_CALLBACK_BATCH = `${Paths.BACKEND_BASE_V2}/payments/callback/batch`;

}

module.exports = Paths; 