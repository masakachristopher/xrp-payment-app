package com.xrp_payment_app.dto;

public record PaymentResponse(
        String status,
        String transactionHash,
        String message
) {}