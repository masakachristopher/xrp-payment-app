package com.xrp_payment_app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xrpl.xrpl4j.client.XrplClient;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoRequestParams;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoResult;
import org.xrpl.xrpl4j.model.client.fees.FeeResult;
import org.xrpl.xrpl4j.model.ledger.AccountRootObject;
import org.xrpl.xrpl4j.model.transactions.Address;

import java.math.BigDecimal;

@Service
public class XrplClientService {
    private static final Logger logger = LoggerFactory.getLogger(XrplClientService.class);

    private final XrplClient xrplClient;

    public XrplClientService(XrplClient xrplClient) {
        this.xrplClient = xrplClient;
    }

    public XrplClient getXrplClient() {
        try {
            return xrplClient;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch XRPL Client", e);
        }
    }

    public BigDecimal getBalance(String address) throws Exception {
        try {
            if (address == null || address.trim().isEmpty()) {
                throw new IllegalArgumentException("Address cannot be null or empty");
            }
            logger.info("Inquiring account balance");
            AccountInfoRequestParams params = AccountInfoRequestParams.of(Address.of(address));
            AccountInfoResult result = xrplClient.accountInfo(params);
            return result.accountData().balance().toXrp();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch balance for " + address);
        }
    }

    public AccountRootObject getAccountData(String address) {
        try {
            if (address == null || address.trim().isEmpty()) {
                throw new IllegalArgumentException("Address cannot be null or empty");
            }
            logger.info("Inquiring account information");
            AccountInfoRequestParams params = AccountInfoRequestParams.of(Address.of(address));
            AccountInfoResult result = xrplClient.accountInfo(params);
            return result.accountData();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch account details for " + address, e);
        }
    }

    public FeeResult getFees() {
        try {
            logger.info("Inquiring payment fees");
            return xrplClient.fee();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch payment fees", e);
        }
    }

}