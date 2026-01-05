package com.xrp_payment_app.dto;

public record XummWebhookEvent(
        String paymentUuid,
        String status
) {}
