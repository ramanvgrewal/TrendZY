package com.trendzy.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class TrendZyException extends RuntimeException {

    private final HttpStatus status;

    // Default: 400 Bad Request
    public TrendZyException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    // With explicit status
    public TrendZyException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public TrendZyException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.BAD_REQUEST;
    }

}