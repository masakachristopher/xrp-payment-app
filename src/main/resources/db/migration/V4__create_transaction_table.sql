CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    xrp_account_id BIGINT NOT NULL REFERENCES xrp_accounts(id),
    destination_address VARCHAR(100) NOT NULL,
    amount DECIMAL(20,6) NOT NULL,
    transaction_hash VARCHAR(100) UNIQUE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);