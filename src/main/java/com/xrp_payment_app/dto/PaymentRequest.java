package com.xrp_payment_app.dto;

import java.math.BigDecimal;

public record PaymentRequest(
        String destinationAddress,
        BigDecimal amount
) {}
