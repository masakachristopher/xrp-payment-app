package com.xrp_payment_app.exception;

import org.springframework.http.HttpStatus;

public class XrpServiceException extends RuntimeException {
    private final String clientMessage;
    private final String errorCode;
    private HttpStatus status;

    public XrpServiceException(String message, Throwable cause, String clientMessage, String errorCode, HttpStatus status) {
        super(message, cause);
        this.clientMessage = clientMessage;
        this.errorCode = errorCode;
        this.status = status;
    }

    public XrpServiceException(String message, Throwable cause, String clientMessage, String errorCode) {
        super(message, cause);
        this.clientMessage = clientMessage;
        this.errorCode = errorCode;
    }

    public XrpServiceException(String message, String clientMessage, String errorCode) {
        super(message);
        this.clientMessage = clientMessage;
        this.errorCode = errorCode;
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
