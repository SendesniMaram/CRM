package com.crm.employee.mapper;

import com.crm.employee.dto.EmployeeRequest;
import com.crm.employee.dto.EmployeeResponse;
import com.crm.employee.entity.Employee;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(Employee employee) {
        if (employee == null) {
            return null;
        }

        EmployeeResponse response = new EmployeeResponse();
        response.setId(employee.getId());
        response.setEmployeeCode(employee.getEmployeeCode());
        response.setFirstName(employee.getFirstName());
        response.setLastName(employee.getLastName());
        response.setEmail(employee.getEmail());
        response.setPhone(employee.getPhone());
        response.setJobTitle(employee.getJobTitle());
        response.setHireDate(employee.getHireDate());
        response.setSalary(employee.getSalary());
        response.setAddress(employee.getAddress());
        response.setDateOfBirth(employee.getDateOfBirth());
        response.setGender(employee.getGender());
        response.setStatus(employee.getStatus());
        response.setCreatedAt(employee.getCreatedAt());
        response.setUpdatedAt(employee.getUpdatedAt());

        return response;
    }

    public Employee toEntity(EmployeeRequest request) {
        if (request == null) {
            return null;
        }

        Employee employee = new Employee();
        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPhone(request.getPhone());
        employee.setJobTitle(request.getJobTitle());
        employee.setHireDate(request.getHireDate());
        employee.setSalary(request.getSalary());
        employee.setAddress(request.getAddress());
        employee.setDateOfBirth(request.getDateOfBirth());
        employee.setGender(request.getGender());
        employee.setStatus(request.getStatus());

        return employee;
    }

    public void updateEntityFromRequest(Employee employee, EmployeeRequest request) {
        if (request == null) {
            return;
        }
        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPhone(request.getPhone());
        employee.setJobTitle(request.getJobTitle());
        employee.setHireDate(request.getHireDate());
        employee.setSalary(request.getSalary());
        employee.setAddress(request.getAddress());
        employee.setDateOfBirth(request.getDateOfBirth());
        employee.setGender(request.getGender());
        employee.setStatus(request.getStatus());
    }
}

