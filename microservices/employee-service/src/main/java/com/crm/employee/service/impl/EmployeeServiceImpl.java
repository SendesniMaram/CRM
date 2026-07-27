package com.crm.employee.service.impl;

import com.crm.employee.dto.EmployeeRequest;
import com.crm.employee.dto.EmployeeResponse;
import com.crm.employee.entity.Employee;
import com.crm.employee.exception.ResourceNotFoundException;
import com.crm.employee.mapper.EmployeeMapper;
import com.crm.employee.repository.EmployeeRepository;
import com.crm.employee.service.IEmployeeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EmployeeServiceImpl implements IEmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                               EmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.employeeMapper = employeeMapper;
    }

    @Override
    public EmployeeResponse create(EmployeeRequest request) {
        Employee employee = employeeMapper.toEntity(request);
        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

@Override
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        employeeMapper.updateEntityFromRequest(existing, request);
        Employee saved = employeeRepository.save(existing);
        return employeeMapper.toResponse(saved);
    }

    @Override
    public EmployeeResponse findById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return employeeMapper.toResponse(employee);
    }

    @Override
    public List<EmployeeResponse> findAll() {
        return employeeRepository.findAll().stream()
                .map(employeeMapper::toResponse)
                .toList();
    }

    @Override
    public void delete(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        employeeRepository.delete(employee);
    }
}
