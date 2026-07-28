package com.crm.employee.exception;

import java.util.stream.Collectors;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.crm.employee.dto.ApiError;

import jakarta.validation.ConstraintViolationException;

/**
 * Centralized exception handler for employee-service.
 * Returns uniform JSON error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<ApiError> handleFeignNotFound(FeignException.NotFound ex, WebRequest request) {
        ApiError body = new ApiError(
                404,
                "Not Found",
                "Department not found: " + ex.getMessage(),
                getPath(request)
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(FeignException.ServiceUnavailable.class)
    public ResponseEntity<ApiError> handleFeignServiceUnavailable(FeignException.ServiceUnavailable ex, WebRequest request) {
        ApiError body = new ApiError(
                503,
                "Service Unavailable",
                "department-service is currently unavailable. Please try again later.",
                getPath(request)
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiError> handleFeignException(FeignException ex, WebRequest request) {
        int status = ex.status() != -1 ? ex.status() : 503;
        ApiError body = new ApiError(
                status,
                "Service Unavailable",
                "department-service is currently unavailable. Please try again later.",
                getPath(request)
        );
        return ResponseEntity.status(status).body(body);
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
