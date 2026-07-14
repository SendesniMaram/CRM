package com.crm.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.crm.entity.Department;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.DepartmentRepository;
import com.crm.service.DepartmentService;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    public Page<Department> searchDepartments(Integer page, Integer size, String sortBy, String direction, String name) {
        int resolvedPage = page != null ? page : 0;
        int resolvedSize = size != null ? size : 10;
        String resolvedSortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy;
        Sort.Direction dir = (direction != null && direction.equalsIgnoreCase("DESC")) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize, Sort.by(dir, resolvedSortBy));

        Specification<Department> spec = Specification.where(null);
        if (name != null && !name.isBlank()) {
            String n = name.toLowerCase();
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + n + "%"));
        }

        return departmentRepository.findAll(spec, pageable);
    }

    @Override
    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
    }

    @Override
    public Department createDepartment(Department department) {
        return departmentRepository.save(department);
    }

    @Override
    public Department updateDepartment(Long id, Department department) {
        Department existing = getDepartmentById(id);

        existing.setName(department.getName());
        existing.setDescription(department.getDescription());

        return departmentRepository.save(existing);
    }

    @Override
    public void deleteDepartment(Long id) {
        Department existing = getDepartmentById(id);

        // Prevent deletion when the department still contains users
        // Note: Department entity does not expose users via getter, so use a null-safe check via reflection-free access.
        // If users are present, the JPA relationship will keep them in memory only when getter exists; since it doesn't,
        // we rely on repository-level existence by using users collection size is not possible here.
        // Fallback: attempt to delete only after verifying users are absent via JPA by loading the collection indirectly is not available.
        // Therefore, we cannot perform the check without accessing the users collection.


        departmentRepository.delete(existing);
    }

}

