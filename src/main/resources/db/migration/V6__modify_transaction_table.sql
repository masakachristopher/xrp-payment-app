ALTER TABLE transactions
ADD COLUMN payment_reference VARCHAR(100),
ADD COLUMN payment_type VARCHAR(50);