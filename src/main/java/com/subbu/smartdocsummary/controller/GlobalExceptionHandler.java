package com.subbu.smartdocsummary.controller;

import com.subbu.smartdocsummary.exception.PresidioClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(PresidioClientException.class)
    public ResponseEntity<String> handlePresidioClientException(PresidioClientException e) {
        log.error("Presidio client error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("PII analysis failed");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("Requested resource not found", e);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Resource not found");
    }
}
