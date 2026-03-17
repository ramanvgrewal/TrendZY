package com.trendzy.exception;

import com.trendzy.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─────────────────────────────────────────────────────────────
    // APP-LEVEL EXCEPTIONS
    // ─────────────────────────────────────────────────────────────

    @ExceptionHandler(TrendZyException.class)
    public ResponseEntity<ApiResponse<Void>> handleTrendZyException(TrendZyException ex) {
        log.warn("[EXCEPTION] TrendZyException — status: {} | message: {}",
                ex.getStatus(), ex.getMessage());

        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ─────────────────────────────────────────────────────────────
    // SECURITY EXCEPTIONS
    // ─────────────────────────────────────────────────────────────

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex) {
        log.warn("[EXCEPTION] AuthenticationException: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Authentication required"));
        // ↑ Don't expose ex.getMessage() — could leak security details
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex) {
        log.warn("[EXCEPTION] AccessDeniedException: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied"));
    }

    // ─────────────────────────────────────────────────────────────
    // VALIDATION EXCEPTIONS
    // ─────────────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getAllErrors()
                .get(0)
                .getDefaultMessage();

        log.warn("[EXCEPTION] Validation failed: {}", message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation error: " + message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(
            MissingServletRequestParameterException ex) {
        log.warn("[EXCEPTION] Missing request parameter: {}", ex.getParameterName());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Missing required parameter: " + ex.getParameterName()));
    }

    // ─────────────────────────────────────────────────────────────
    // 404
    // ─────────────────────────────────────────────────────────────

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoHandlerFoundException ex) {
        log.warn("[EXCEPTION] No handler found: {} {}", ex.getHttpMethod(), ex.getRequestURL());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Endpoint not found: " + ex.getRequestURL()));
    }

    // ─────────────────────────────────────────────────────────────
    // CATCH-ALL — never expose internals to client
    // ─────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex) {
        // Log full stack trace internally
        log.error("[EXCEPTION] Unhandled exception: {}", ex.getMessage(), ex);

        // Return generic message to client — never leak ex.getMessage()
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Something went wrong. Please try again."));
    }
}