package com.crm.service;

import org.springframework.data.domain.Page;

import com.crm.dto.EmployeeRequest;
import com.crm.dto.EmployeeResponse;

public interface EmployeeService {

    Page<EmployeeResponse> searchEmployees(Integer page,
                                            Integer size,
                                            String sortBy,
                                            String direction,
                                            String employeeCode,
                                            String jobTitle,
                                            Long departmentId,
                                            com.crm.enums.EmployeeStatus status,
                                            com.crm.enums.Gender gender);

    EmployeeResponse getEmployeeById(Long id);

    EmployeeResponse createEmployee(EmployeeRequest request);

    EmployeeResponse updateEmployee(Long id, EmployeeRequest request);

    void deleteEmployee(Long id);
}

