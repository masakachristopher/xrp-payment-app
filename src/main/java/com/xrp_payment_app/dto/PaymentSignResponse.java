package com.xrp_payment_app.dto;

public record PaymentSignResponse(
        String requestId,
        String status,
        String userUuid,
        String userRedirectUrl,
        String feeUuid,
        String feeRedirectUrl,
        String message
) {}