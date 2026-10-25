package com.crm.employee.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * FallbackFactory for DepartmentClient using Resilience4j Circuit Breaker.
 * Refuses dependency failures instead of returning normal-looking missing data.
 */
@Component
public class DepartmentClientFallbackFactory implements FallbackFactory<DepartmentClient> {

    @Override
    public DepartmentClient create(Throwable cause) {
        return new DepartmentClientFallback(cause);
    }
}
