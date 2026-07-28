package com.crm.employee.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * FallbackFactory for DepartmentClient using Resilience4j Circuit Breaker.
 * Returns a fallback DepartmentClient that logs the error and returns null department data.
 */
@Component
public class DepartmentClientFallbackFactory implements FallbackFactory<DepartmentClient> {

    private static final Logger log = LoggerFactory.getLogger(DepartmentClientFallbackFactory.class);

    @Override
    public DepartmentClient create(Throwable cause) {
        log.error("Circuit Breaker triggered for department-service. Cause: {}", cause.getMessage());
        return new DepartmentClientFallback(cause);
    }
}
