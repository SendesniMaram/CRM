package com.crm.customer.exception;

import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.crm.customer.dto.ApiError;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(org.springframework.dao.DataIntegrityViolationException ex, WebRequest request) {
        return ResponseEntity.status(409).cacheControl(org.springframework.http.CacheControl.noStore())
                .body(new ApiError(409, "Conflict", "Operation conflicts with an existing reference", getPath(request)));
    }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ApiError> handleStatus(org.springframework.web.server.ResponseStatusException ex, WebRequest request) {
        int status = ex.getStatusCode().value();
        return ResponseEntity.status(status).cacheControl(org.springframework.http.CacheControl.noStore())
                .body(new ApiError(status, "Request unavailable", "Customer access unavailable", getPath(request)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        ApiError body = new ApiError(
                400,
                "Bad Request",
                ex.getMessage() != null ? ex.getMessage() : "Invalid request",
                getPath(request)
        );
        return ResponseEntity.status(400).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ApiError body = new ApiError(
                400,
                "Bad Request",
                "Validation failed: " + details,
                getPath(request)
        );
        return ResponseEntity.status(400).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        ApiError body = new ApiError(
                400,
                "Bad Request",
                ex.getMessage() != null ? ex.getMessage() : "Constraint violation",
                getPath(request)
        );
        return ResponseEntity.status(400).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        ApiError body = new ApiError(
                403,
                "Forbidden",
                "Access denied",
                getPath(request)
        );
        return ResponseEntity.status(403).body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        ApiError body = new ApiError(
                404,
                "Not Found",
                ex.getMessage() != null ? ex.getMessage() : "Resource not found",
                getPath(request)
        );
        return ResponseEntity.status(404).body(body);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(org.springframework.web.servlet.resource.NoResourceFoundException ex, WebRequest request) {
        ApiError body = new ApiError(
                404,
                "Not Found",
                "Resource not found",
                getPath(request)
        );
        return ResponseEntity.status(404).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, WebRequest request) {
        ApiError body = new ApiError(
                500,
                "Internal Server Error",
                "An unexpected error occurred",
                getPath(request)
        );
        return ResponseEntity.status(500).body(body);
    }

    private String getPath(WebRequest request) {
        if (request instanceof ServletWebRequest swr) {
            return swr.getRequest().getRequestURI();
        }
        return request.getDescription(false);
    }
}

