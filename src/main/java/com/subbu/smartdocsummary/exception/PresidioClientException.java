package com.subbu.smartdocsummary.exception;

public class PresidioClientException extends RuntimeException {
    public PresidioClientException(String message) {
        super(message);
    }

    public PresidioClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
