package com.crm.employee.service;

import com.crm.employee.dto.EmployeeRequest;
import com.crm.employee.dto.EmployeeResponse;
import com.crm.employee.dto.EmployeeWithDepartmentResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IEmployeeService {

    EmployeeResponse createEmployee(EmployeeRequest request);

    EmployeeResponse getEmployeeById(Long id);

    List<EmployeeResponse> getAllEmployees();

    Page<EmployeeResponse> getAllEmployeesPaged(int page, int size, String sortBy, String direction, String keyword);

    EmployeeResponse updateEmployee(Long id, EmployeeRequest request);

    void deleteEmployee(Long id);

    EmployeeWithDepartmentResponse getEmployeeWithDepartment(Long id);
}

