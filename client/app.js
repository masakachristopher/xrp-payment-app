const express = require('express');
const session = require('./middleware/session');

const paymentRoutes = require('./route/paymentRoutes');
const pageRoutes = require('./route/pageRoutes');

const app = express();

app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(session);

app.use('/', pageRoutes);
app.use('/payment', paymentRoutes);

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
