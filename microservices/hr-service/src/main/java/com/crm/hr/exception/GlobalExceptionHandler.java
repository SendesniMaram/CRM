package com.crm.hr.exception;

import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.crm.hr.dto.ApiError;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(Exception ex, WebRequest request) {
        return ResponseEntity.badRequest().body(new ApiError(400, "Bad Request", "Invalid request body", getPath(request)));
    }

    @ExceptionHandler(LeaveAssociationConflictException.class)
    public ResponseEntity<ApiError> handleAssociationConflict(LeaveAssociationConflictException ex, WebRequest request) {
        return ResponseEntity.status(409).body(new ApiError(409, "Conflict", ex.getMessage(), getPath(request)));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiError> handleLeaveAccessDenied(org.springframework.security.access.AccessDeniedException ex, WebRequest request) {
        return ResponseEntity.status(403).body(new ApiError(403, "Forbidden", ex.getMessage(), getPath(request)));
    }

    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleLeaveConflict(Exception ex, WebRequest request) {
        return ResponseEntity.status(409).body(new ApiError(409, "Conflict", "The request has already changed; reload it", getPath(request)));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ApiError> handleLeaveDirectory(org.springframework.web.server.ResponseStatusException ex, WebRequest request) {
        int status = ex.getStatusCode().value();
        String message = switch (status) {
              case 401 -> "Leave directory authentication failed";
            case 403 -> "Leave directory access denied";
            case 404 -> "Directory resource not found";
            case 502 -> "Leave directory communication failed";
            default -> "Leave directory unavailable";
        };
        String error = org.springframework.http.HttpStatus.valueOf(status).getReasonPhrase();
        return ResponseEntity.status(status).body(new ApiError(status, error, message, getPath(request)));
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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Data integrity violation";
        if (message.contains("Unique index") || message.contains("unique") || message.contains("Duplicate")) {
            message = "A record with the same unique value already exists";
        }
        ApiError body = new ApiError(
                400,
                "Bad Request",
                message,
                getPath(request)
        );
        return ResponseEntity.status(400).body(body);
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
