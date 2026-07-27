package com.crm.department.service;

import com.crm.department.dto.DepartmentRequest;
import com.crm.department.dto.DepartmentResponse;

import java.util.List;

public interface IDepartmentService {

    DepartmentResponse createDepartment(DepartmentRequest request);

    DepartmentResponse getDepartmentById(Long id);

    List<DepartmentResponse> getAllDepartments();

    DepartmentResponse updateDepartment(Long id, DepartmentRequest request);

    void deleteDepartment(Long id);
}
