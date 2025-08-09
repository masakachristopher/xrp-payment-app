package com.xrp_payment_app.dto;

public record ApiErrorResponse(
        String message,
        String errorCode,
        int status,
        String path,
        long timestamp
) {}
