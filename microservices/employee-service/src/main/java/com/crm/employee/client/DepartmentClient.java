package com.crm.employee.client;

import com.crm.employee.client.dto.DepartmentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "department-service",
    path = "/api/departments",
    fallbackFactory = DepartmentClientFallbackFactory.class
)
public interface DepartmentClient {

    @GetMapping("/{id}")
    DepartmentResponse getDepartmentById(@PathVariable("id") Long id);
}
