package com.crm.employee.client;

import com.crm.employee.client.dto.DepartmentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fallback implementation for DepartmentClient when department-service is unavailable.
 */
public class DepartmentClientFallback implements DepartmentClient {

    private static final Logger log = LoggerFactory.getLogger(DepartmentClientFallback.class);

    private final Throwable cause;

    public DepartmentClientFallback(Throwable cause) {
        this.cause = cause;
    }

    @Override
    public DepartmentResponse getDepartmentById(Long id) {
        log.warn("Fallback: department-service is unavailable. Returning null department for employee id: {}. Cause: {}",
                id, cause.getMessage());
        return null;
    }
}
