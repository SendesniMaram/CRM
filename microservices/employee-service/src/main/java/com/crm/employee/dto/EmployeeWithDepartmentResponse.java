package com.crm.employee.dto;

import com.crm.employee.client.dto.DepartmentResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeWithDepartmentResponse {

    private EmployeeResponse employee;

    private DepartmentResponse department;
}

