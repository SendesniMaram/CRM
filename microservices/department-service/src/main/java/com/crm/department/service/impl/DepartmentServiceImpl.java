package com.crm.department.service.impl;

import com.crm.department.dto.DepartmentRequest;
import com.crm.department.dto.DepartmentResponse;
import com.crm.department.entity.Department;
import com.crm.department.exception.ResourceNotFoundException;
import com.crm.department.mapper.DepartmentMapper;
import com.crm.department.repository.DepartmentRepository;
import com.crm.department.service.IDepartmentService;
import com.crm.department.util.DepartmentSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
public class DepartmentServiceImpl implements IDepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository,
                                  DepartmentMapper departmentMapper) {
        this.departmentRepository = departmentRepository;
        this.departmentMapper = departmentMapper;
    }

    @Override
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Department name already exists: " + request.getName());
        }
        if (StringUtils.hasText(request.getCode())
                && departmentRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Department code already exists: " + request.getCode());
        }

        Department department = departmentMapper.toEntity(request);
        Department saved = departmentRepository.save(department);
        return departmentMapper.toResponse(saved);
    }

    @Override
    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        return departmentMapper.toResponse(department);
    }

    @Override
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(departmentMapper::toResponse)
                .toList();
    }

    @Override
    public Page<DepartmentResponse> getAllDepartmentsPaged(int page, int size, String sortBy, String direction, String keyword) {
        String sortField = StringUtils.hasText(sortBy) ? sortBy : "id";
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(sortDirection, sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Department> departmentPage;

        if (StringUtils.hasText(keyword)) {
            departmentPage = departmentRepository.findAll(
                    DepartmentSpecification.searchByKeyword(keyword), pageable);
        } else {
            departmentPage = departmentRepository.findAll(pageable);
        }

        return departmentPage.map(departmentMapper::toResponse);
    }

    @Override
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        if (!existing.getName().equals(request.getName())
                && departmentRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Department name already exists: " + request.getName());
        }
        if (StringUtils.hasText(request.getCode())
                && !request.getCode().equals(existing.getCode())
                && departmentRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Department code already exists: " + request.getCode());
        }

        departmentMapper.updateEntityFromRequest(existing, request);
        Department saved = departmentRepository.save(existing);
        return departmentMapper.toResponse(saved);
    }

    @Override
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        departmentRepository.delete(department);
    }
}
