package com.xrp_payment_app.dto;

public record SuccessPaymentResponse(
        String status,
        String userTxId,
        String feeTxId,
        String message
) {}