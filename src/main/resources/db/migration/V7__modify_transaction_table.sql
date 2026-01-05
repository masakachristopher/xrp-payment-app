ALTER TABLE transactions
ADD COLUMN platform_fee DECIMAL(20,6),
ADD COLUMN network_fee DECIMAL(20,6);