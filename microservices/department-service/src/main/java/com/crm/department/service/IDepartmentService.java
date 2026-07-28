package com.crm.department.service;

import com.crm.department.dto.DepartmentRequest;
import com.crm.department.dto.DepartmentResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IDepartmentService {

    DepartmentResponse createDepartment(DepartmentRequest request);

    DepartmentResponse getDepartmentById(Long id);

    List<DepartmentResponse> getAllDepartments();

    Page<DepartmentResponse> getAllDepartmentsPaged(int page, int size, String sortBy, String direction, String keyword);

    DepartmentResponse updateDepartment(Long id, DepartmentRequest request);

    void deleteDepartment(Long id);
}
