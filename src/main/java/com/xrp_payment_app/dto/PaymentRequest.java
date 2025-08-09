package com.xrp_payment_app.dto;

import java.math.BigDecimal;

public record PaymentRequest(
        String userId,
        String destinationAddress,
        BigDecimal amount
) {}
