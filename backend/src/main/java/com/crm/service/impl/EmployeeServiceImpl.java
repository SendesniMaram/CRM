package com.crm.service.impl;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.crm.dto.EmployeeRequest;
import com.crm.dto.EmployeeResponse;
import com.crm.entity.Department;
import com.crm.entity.Employee;
import com.crm.entity.User;
import com.crm.enums.EmployeeStatus;
import com.crm.enums.Gender;
import com.crm.exception.ResourceNotFoundException;
import com.crm.mapper.EmployeeMapper;
import com.crm.repository.DepartmentRepository;
import com.crm.repository.EmployeeRepository;
import com.crm.repository.UserRepository;
import com.crm.service.EmployeeService;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final EmployeeMapper employeeMapper;

    public EmployeeServiceImpl(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            EmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.employeeMapper = employeeMapper;
    }

    @Override
    public Page<EmployeeResponse> searchEmployees(Integer page,
                                                    Integer size,
                                                    String sortBy,
                                                    String direction,
                                                    String employeeCode,
                                                    String jobTitle,
                                                    Long departmentId,
                                                    EmployeeStatus status,
                                                    Gender gender) {

        int resolvedPage = page != null ? page : 0;
        int resolvedSize = size != null ? size : 10;
        String resolvedSortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy;
        Sort.Direction dir = (direction != null && direction.equalsIgnoreCase("DESC")) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize, Sort.by(dir, resolvedSortBy));

        Specification<Employee> spec = Specification.where(null);

        if (employeeCode != null && !employeeCode.isBlank()) {
            String code = employeeCode.toLowerCase();
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("employeeCode")), "%" + code + "%"));
        }

        if (jobTitle != null && !jobTitle.isBlank()) {
            String jt = jobTitle.toLowerCase();
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("jobTitle")), "%" + jt + "%"));
        }

        if (departmentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        if (gender != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("gender"), gender));
        }

        return employeeRepository.findAll(spec, pageable).map(employeeMapper::toResponse);
    }

    @Override
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return employeeMapper.toResponse(employee);
    }

    @Override
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        Employee employee = employeeMapper.toEntity(request, department, user);
        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    @Override
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        employeeMapper.updateEntityFromRequest(existing, request, department, user);

        Employee saved = employeeRepository.save(existing);
        return employeeMapper.toResponse(saved);
    }

    @Override
    public void deleteEmployee(Long id) {
        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        employeeRepository.delete(existing);
    }
}

