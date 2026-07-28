package com.crm.employee.service.impl;

import com.crm.employee.client.DepartmentClient;
import com.crm.employee.client.dto.DepartmentResponse;
import com.crm.employee.dto.EmployeeRequest;
import com.crm.employee.dto.EmployeeResponse;
import com.crm.employee.dto.EmployeeWithDepartmentResponse;
import com.crm.employee.entity.Employee;
import com.crm.employee.exception.ResourceNotFoundException;
import com.crm.employee.mapper.EmployeeMapper;
import com.crm.employee.repository.EmployeeRepository;
import com.crm.employee.service.IEmployeeService;
import com.crm.employee.util.EmployeeSpecification;
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
public class EmployeeServiceImpl implements IEmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final DepartmentClient departmentClient;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                               EmployeeMapper employeeMapper,
                               DepartmentClient departmentClient) {
        this.employeeRepository = employeeRepository;
        this.employeeMapper = employeeMapper;
        this.departmentClient = departmentClient;
    }

    @Override
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        // Validation : employeeCode unique
        if (employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new IllegalArgumentException(
                    "Employee code already exists: " + request.getEmployeeCode());
        }
        // Validation : email unique (si renseigné)
        if (StringUtils.hasText(request.getEmail())
                && employeeRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Email already exists: " + request.getEmail());
        }

        Employee employee = employeeMapper.toEntity(request);
        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    @Override
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return employeeMapper.toResponse(employee);
    }

    @Override
    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(employeeMapper::toResponse)
                .toList();
    }

    @Override
    public Page<EmployeeResponse> getAllEmployeesPaged(int page, int size, String sortBy, String direction, String keyword) {
        // Default sort field
        String sortField = StringUtils.hasText(sortBy) ? sortBy : "id";
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(sortDirection, sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Employee> employeePage;

        if (StringUtils.hasText(keyword)) {
            employeePage = employeeRepository.findAll(
                    EmployeeSpecification.searchByKeyword(keyword), pageable);
        } else {
            employeePage = employeeRepository.findAll(pageable);
        }

        return employeePage.map(employeeMapper::toResponse);
    }

    @Override
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        // Validation : employeeCode unique (exclure l'employé courant)
        if (!existing.getEmployeeCode().equals(request.getEmployeeCode())
                && employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new IllegalArgumentException(
                    "Employee code already exists: " + request.getEmployeeCode());
        }
        // Validation : email unique (exclure l'employé courant, si email renseigné)
        if (StringUtils.hasText(request.getEmail())
                && !request.getEmail().equals(existing.getEmail())
                && employeeRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Email already exists: " + request.getEmail());
        }

        employeeMapper.updateEntityFromRequest(existing, request);
        Employee saved = employeeRepository.save(existing);
        return employeeMapper.toResponse(saved);
    }

    @Override
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        employeeRepository.delete(employee);
    }

    @Override
    public EmployeeWithDepartmentResponse getEmployeeWithDepartment(Long id) {
        EmployeeResponse employee = getEmployeeById(id);
        // Circuit Breaker + Fallback gère automatiquement :
        // - Si department-service répond -> département retourné
        // - Si department-service indisponible -> fallback retourne null
        // - Si département inexistant (404) -> FeignException propagée à GlobalExceptionHandler
        DepartmentResponse department = departmentClient.getDepartmentById(id);
        return new EmployeeWithDepartmentResponse(employee, department);
    }
}
