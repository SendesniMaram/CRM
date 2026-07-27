package com.crm.employee.service;

import com.crm.employee.dto.EmployeeRequest;
import com.crm.employee.dto.EmployeeResponse;

import java.util.List;

public interface IEmployeeService {

    EmployeeResponse create(EmployeeRequest request);

    EmployeeResponse update(Long id, EmployeeRequest request);

    EmployeeResponse findById(Long id);

    List<EmployeeResponse> findAll();

    void delete(Long id);
}

