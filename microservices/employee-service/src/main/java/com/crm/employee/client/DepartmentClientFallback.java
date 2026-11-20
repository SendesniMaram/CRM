package com.crm.employee.client;

import com.crm.employee.client.dto.DepartmentResponse;

/**
 * Fallback implementation for DepartmentClient when department-service is unavailable.
 */
public class DepartmentClientFallback implements DepartmentClient {

    private final Throwable cause;

    public DepartmentClientFallback(Throwable cause) {
        this.cause = cause;
    }

    @Override
    public DepartmentResponse getDepartmentById(Long id) {
        if (cause instanceof feign.FeignException failure && (failure.status() == 401 || failure.status() == 403 || failure.status() == 404)) {
            throw failure;
        }
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Department validation unavailable");
    }
}
