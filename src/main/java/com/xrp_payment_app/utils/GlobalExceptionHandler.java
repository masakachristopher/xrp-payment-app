package com.xrp_payment_app.utils;

import com.xrp_payment_app.dto.ApiErrorResponse;
import com.xrp_payment_app.dto.ErrorResponse;
import com.xrp_payment_app.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.UUID;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        logger.error("TraceId: {}, ErrorCode: {}, ErrorMessage: {}", traceId, ex.getErrorCode(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now().toString(),
                HttpStatus.BAD_REQUEST.value(),
                ex.getErrorCode(),
                ex.getClientMessage(),
                request.getRequestURI(),
                traceId
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnprocessedException.class)
    public ResponseEntity<ErrorResponse> handleUnprocessedException(UnprocessedException ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        logger.error("TraceId: {}, ErrorCode: {}, ErrorMessage: {}", traceId, ex.getErrorCode(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now().toString(),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                ex.getErrorCode(),
                ex.getClientMessage(),
                request.getRequestURI(),
                traceId
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(NotFoundException ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        logger.error("TraceId: {}, ErrorCode: {}, ErrorMessage: {}", traceId, ex.getErrorCode(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now().toString(),
                HttpStatus.NOT_FOUND.value(),
                ex.getErrorCode(),
                ex.getClientMessage(),
                request.getRequestURI(),
                traceId
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(XrpServiceException.class)
    public ResponseEntity<ErrorResponse> handleXrpServiceException(XrpServiceException ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        logger.error("TraceId: {}, ErrorCode: {}, ErrorMessage: {}", traceId, ex.getErrorCode(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now().toString(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI(),
                traceId
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralError(Exception ex, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now().toString(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "An unexpected error occurred.",
                request.getRequestURI(),
                UUID.randomUUID().toString()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
