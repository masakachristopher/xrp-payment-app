package com.xrp_payment_app.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends XrpServiceException {
    public NotFoundException(String message, Throwable cause, String clientMessage, String errorCode, HttpStatus status) {
        super(message, cause, clientMessage, errorCode, status);
    }

    public NotFoundException(String message, Throwable cause, String clientMessage, String errorCode) {
        super(message, cause, clientMessage, errorCode, HttpStatus.NOT_FOUND);
    }

    public NotFoundException(String message, String clientMessage, String errorCode) {
        super(message, null, clientMessage, errorCode, HttpStatus.NOT_FOUND);
    }

    public NotFoundException(String message) {
        super(message, null, "The resource doesn't exist", "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
