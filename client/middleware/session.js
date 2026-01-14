const session = require('express-session');

module.exports = session({
    secret: process.env.SESSION_SECRET || 'xaman-secret',
    resave: false,
    saveUninitialized: false,
    cookie: {
        httpOnly: true,
        // secure: false // true in HTTPS
    }
});
