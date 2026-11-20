package com.crm.department.mapper;

import com.crm.department.dto.DepartmentRequest;
import com.crm.department.dto.DepartmentResponse;
import com.crm.department.entity.Department;
import org.springframework.stereotype.Component;

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
        response.setCode(department.getCode());
        response.setCreatedAt(department.getCreatedAt());
        response.setUpdatedAt(department.getUpdatedAt());

        return response;
    }

    public Department toEntity(DepartmentRequest request) {
        if (request == null) {
            return null;
        }

        Department department = new Department();
        department.setName(request.getName());
        department.setDescription(request.getDescription());
        department.setCode(request.getCode());

        return department;
    }

    public void updateEntityFromRequest(Department department, DepartmentRequest request) {
        if (request == null) {
            return;
        }
        department.setName(request.getName());
        department.setDescription(request.getDescription());
        department.setCode(request.getCode());
    }
}
