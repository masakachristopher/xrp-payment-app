package com.xrp_payment_app.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends XrpServiceException{
    public BadRequestException(String message, Throwable cause, String clientMessage, String errorCode, HttpStatus status) {
        super(message, cause, clientMessage, errorCode, status);
    }

    public BadRequestException(String message, Throwable cause, String clientMessage, String errorCode) {
        super(message, cause, clientMessage, errorCode, HttpStatus.BAD_REQUEST);
    }

    public BadRequestException(String message, String clientMessage, String errorCode) {
        super(message, null, clientMessage, errorCode, HttpStatus.BAD_REQUEST);
    }
}
