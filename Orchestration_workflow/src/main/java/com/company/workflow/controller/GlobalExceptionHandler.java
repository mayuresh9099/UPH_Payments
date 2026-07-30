package com.company.workflow.controller;

import com.company.workflow.exception.RetryLimitExceededException;
import com.company.workflow.exception.WorkflowException;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RetryLimitExceededException.class)
    ResponseEntity<ApiError> retryLimitExceeded(RetryLimitExceededException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(WorkflowException.class)
    ResponseEntity<ApiError> workflowException(WorkflowException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        return error(HttpStatus.BAD_REQUEST, "Invalid workflow request");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> illegalArgument(IllegalArgumentException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected workflow error");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message));
    }
}
