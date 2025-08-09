package com.xrp_payment_app.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final String clientMessage;
    private final String errorCode;
    private final HttpStatus status;

    public ApiException(String message, Throwable cause, String clientMessage, String errorCode, HttpStatus status) {
        super(message, cause);
        this.clientMessage = clientMessage;
        this.errorCode = errorCode;
        this.status = status;
    }

    public String getClientMessage() {
        return clientMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
