package com.crm.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.crm.entity.Department;

public interface DepartmentService {

    Page<Department> searchDepartments(Integer page,
                                       Integer size,
                                       String sortBy,
                                       String direction,
                                       String name);

    Department getDepartmentById(Long id);

    Department createDepartment(Department department);

    Department updateDepartment(Long id, Department department);

    void deleteDepartment(Long id);
}

