package com.xrp_payment_app.dto;

public record ErrorResponse(
        String timestamp,
        int status,
        String errorCode,
        String message,
        String path,
        String traceId
) {}

