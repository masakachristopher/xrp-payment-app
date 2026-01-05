package com.xrp_payment_app.dto;

public record SubmitResultResponse(
        String engineResult,           
        String engineResultMessage,   
        String transactionHash,     
        boolean isTentativelyAccepted
) {}