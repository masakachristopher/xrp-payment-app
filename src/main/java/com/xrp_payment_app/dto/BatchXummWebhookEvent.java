package com.xrp_payment_app.dto;

import java.util.List;

public record BatchXummWebhookEvent(
        List<String> paymentUuids,
        String status
) {}
