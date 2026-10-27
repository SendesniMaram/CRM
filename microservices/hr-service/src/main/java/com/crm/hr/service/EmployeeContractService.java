package com.crm.hr.service;

import com.crm.hr.dto.EmployeeContractRequest;
import com.crm.hr.entity.EmployeeContract;
import com.crm.hr.exception.ResourceNotFoundException;
import com.crm.hr.repository.EmployeeContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EmployeeContractService {
    private final EmployeeContractRepository repository;
    private final EmployeeReferenceValidator employees;
    public EmployeeContractService(EmployeeContractRepository repository, EmployeeReferenceValidator employees) { this.repository = repository; this.employees = employees; }

    @Transactional(readOnly = true)
    public List<EmployeeContract> list(String employeeId) {
        return employeeId == null || employeeId.isBlank()
                ? repository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "startDate"))
                : repository.findByEmployeeIdOrderByStartDateDescIdDesc(employeeId.trim());
    }
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<EmployeeContract> page(int page, int size, String keyword, String employeeId) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid pagination");
        var specification = (org.springframework.data.jpa.domain.Specification<EmployeeContract>) (root, query, cb) -> cb.conjunction();
        if (employeeId != null && !employeeId.isBlank()) specification = specification.and((root, query, cb) -> cb.equal(root.get("employeeId"), employeeId.trim()));
        if (keyword != null && !keyword.isBlank()) {
            String value = "%" + keyword.trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) -> cb.or(cb.like(cb.lower(root.get("employeeId")), value), cb.like(cb.lower(root.get("contractType")), value), cb.like(cb.lower(root.get("status").as(String.class)), value)));
        }
        return repository.findAll(specification, org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "startDate")
                        .and(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"))));
    }
    @Transactional(readOnly = true)
    public EmployeeContract get(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Contract not found")); }
    public EmployeeContract create(EmployeeContractRequest request,String authorization) { employees.requireExists(request.employeeId().trim(),authorization); return repository.save(toEntity(new EmployeeContract(), request)); }
    public EmployeeContract update(Long id, EmployeeContractRequest request,String authorization) { employees.requireExists(request.employeeId().trim(),authorization); return repository.save(toEntity(get(id), request)); }
    public void delete(Long id) {
        EmployeeContract contract = get(id);
        if (repository.existsByParentContractId(id)) throw new IllegalArgumentException("Contract with amendments cannot be deleted");
        repository.delete(contract);
    }
    private EmployeeContract toEntity(EmployeeContract entity, EmployeeContractRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate()))
            throw new IllegalArgumentException("Contract end date cannot precede start date");
        if (request.parentContractId() != null) {
            EmployeeContract parent = get(request.parentContractId());
            if (!parent.getEmployeeId().equals(request.employeeId())) throw new IllegalArgumentException("Amendment must belong to the same employee as its contract");
            java.util.Set<Long> visited = new java.util.HashSet<>();
            while (parent != null) {
                if (entity.getId() != null && entity.getId().equals(parent.getId())) throw new IllegalArgumentException("Contract amendment cannot create a cycle");
                if (!visited.add(parent.getId()) || parent.getParentContractId() == null) break;
                parent = get(parent.getParentContractId());
            }
        }
        entity.setEmployeeId(request.employeeId().trim()); entity.setContractType(request.contractType().trim());
        entity.setStartDate(request.startDate()); entity.setEndDate(request.endDate()); entity.setStatus(request.status());
        entity.setParentContractId(request.parentContractId()); entity.setNotes(request.notes());
        return entity;
    }
}
