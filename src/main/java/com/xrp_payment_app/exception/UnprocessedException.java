package com.xrp_payment_app.exception;

import org.springframework.http.HttpStatus;

public class UnprocessedException extends XrpServiceException {
    public UnprocessedException(String message, Throwable cause, String clientMessage, String errorCode, HttpStatus status) {
        super(message, cause, clientMessage, errorCode, status);
    }

    public UnprocessedException(String message, Throwable cause, String clientMessage, String errorCode) {
        super(message, cause, clientMessage, errorCode, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public UnprocessedException(String message, String clientMessage, String errorCode) {
        super(message, null, clientMessage, errorCode, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public UnprocessedException(String message) {
        super(message, null, "The request could not be processed due to a business rule violation", "UNPROCESSED_ENTITY", HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
