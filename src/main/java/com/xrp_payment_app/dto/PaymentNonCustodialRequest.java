package com.xrp_payment_app.dto;

import java.math.BigDecimal;

public record PaymentNonCustodialRequest(
        String userName,
        String senderAddress,
        String destinationAddress,
        BigDecimal amount,
        boolean batch
) {}
