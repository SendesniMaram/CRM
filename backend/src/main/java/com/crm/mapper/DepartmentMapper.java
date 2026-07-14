package com.crm.mapper;

import org.springframework.stereotype.Component;

import com.crm.dto.DepartmentRequest;
import com.crm.dto.DepartmentResponse;
import com.crm.entity.Department;

@Component
public class DepartmentMapper {

    public DepartmentResponse toResponse(Department department) {
        if (department == null) {
            return null;
        }

        DepartmentResponse response = new DepartmentResponse();
        response.setId(department.getId());
        response.setName(department.getName());
        response.setDescription(department.getDescription());
        return response;
    }

    public Department toEntity(DepartmentRequest request) {
        if (request == null) {
            return null;
        }

        Department department = new Department();
        department.setName(request.getName());
        department.setDescription(request.getDescription());
        return department;
    }
}

