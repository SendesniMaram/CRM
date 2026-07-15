package com.crm.mapper;

import org.springframework.stereotype.Component;

import com.crm.dto.EmployeeRequest;
import com.crm.dto.EmployeeResponse;
import com.crm.entity.Department;
import com.crm.entity.Employee;
import com.crm.entity.User;

@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(Employee employee) {
        if (employee == null) {
            return null;
        }

        EmployeeResponse response = new EmployeeResponse();
        response.setId(employee.getId());
        response.setEmployeeCode(employee.getEmployeeCode());
        response.setJobTitle(employee.getJobTitle());
        response.setHireDate(employee.getHireDate());
        response.setSalary(employee.getSalary());
        response.setAddress(employee.getAddress());
        response.setDateOfBirth(employee.getDateOfBirth());
        response.setGender(employee.getGender());
        response.setStatus(employee.getStatus());
        response.setCreatedAt(employee.getCreatedAt());
        response.setUpdatedAt(employee.getUpdatedAt());

        Department department = employee.getDepartment();
        if (department != null) {
            response.setDepartmentId(department.getId());
            response.setDepartmentName(department.getName());
        }

        User user = employee.getUser();
        if (user != null) {
            response.setUserId(user.getId());
            response.setUsername(user.getUsername());
        }

        return response;
    }

    public Employee toEntity(EmployeeRequest request, Department department, User user) {
        if (request == null) {
            return null;
        }

        Employee employee = new Employee();
        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setJobTitle(request.getJobTitle());
        employee.setHireDate(request.getHireDate());
        employee.setSalary(request.getSalary());
        employee.setAddress(request.getAddress());
        employee.setDateOfBirth(request.getDateOfBirth());
        employee.setGender(request.getGender());
        employee.setStatus(request.getStatus());
        employee.setDepartment(department);
        employee.setUser(user);

        return employee;
    }

    public void updateEntityFromRequest(Employee employee, EmployeeRequest request, Department department, User user) {
        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setJobTitle(request.getJobTitle());
        employee.setHireDate(request.getHireDate());
        employee.setSalary(request.getSalary());
        employee.setAddress(request.getAddress());
        employee.setDateOfBirth(request.getDateOfBirth());
        employee.setGender(request.getGender());
        employee.setStatus(request.getStatus());
        employee.setDepartment(department);
        employee.setUser(user);
    }
}

