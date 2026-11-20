package com.crm.hr.controller;

import com.crm.hr.client.LeaveDirectoryClient;
import com.crm.hr.client.DirectoryAccount;
import com.crm.hr.entity.LeaveEmployeeAssociation;
import com.crm.hr.exception.LeaveAssociationConflictException;
import com.crm.hr.exception.ResourceNotFoundException;
import com.crm.hr.repository.LeaveEmployeeAssociationRepository;
import com.crm.hr.repository.LeaveRepository;
import com.crm.hr.service.LeaveAccess;
import com.crm.hr.service.EmployeeAssociationService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpHeaders;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/leaves/associations")
public class LeaveAssociationController {
    private final LeaveDirectoryClient directory;
    private final LeaveEmployeeAssociationRepository associations;
    private final LeaveAccess access;
    private final LeaveRepository leaves;
    private final EmployeeAssociationService associationService;
    public LeaveAssociationController(LeaveDirectoryClient directory, LeaveEmployeeAssociationRepository associations, LeaveAccess access, LeaveRepository leaves, EmployeeAssociationService associationService) {
        this.directory = directory; this.associations = associations; this.access = access; this.leaves = leaves; this.associationService = associationService;
    }
    public record LinkRequest(@NotNull @Positive Long userId, @NotNull @Positive Long employeeId) {}
    public record ReassignmentRequest(@NotNull @Positive Long employeeId) {}
    @GetMapping
    public List<LeaveEmployeeAssociation> list() { return associationService.list(); }
    @GetMapping("/options")
    public Map<String, List<Map<String, Object>>> options(@RequestHeader(HttpHeaders.AUTHORIZATION) String bearer) {
        access.requireAdmin();
        JsonNode users = directory.read("identity-service", "/api/users", bearer);
        JsonNode employees = directory.read("employee-service", "/api/employees", bearer);
        DirectoryAccount.requireArray(users);
        DirectoryAccount.requireArray(employees);
        return Map.of("users", StreamSupport.stream(users.spliterator(), false)
                .filter(DirectoryAccount::eligibleEmployee)
                .map(u -> Map.<String,Object>of("id", u.path("id").asLong(), "label", u.path("username").asText())).toList(),
                "employees", StreamSupport.stream(employees.spliterator(), false)
                .map(e -> Map.<String,Object>of("id", e.path("id").asLong(), "label", e.path("employeeCode").asText() + " — " + e.path("firstName").asText() + " " + e.path("lastName").asText())).toList());
    }
    @PostMapping
    public LeaveEmployeeAssociation link(@Valid @RequestBody LinkRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearer) {
        try {
            return associationService.create(request.userId(), request.employeeId(), bearer).association();
        } catch (LeaveAssociationConflictException ex) {
            // Preserve the legacy POST's 400 contract; the new API exposes conflicts as 409.
            throw new IllegalArgumentException("The association conflicts with an existing record");
        }
    }

    @PutMapping("/{userId}")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LeaveEmployeeAssociation reassign(@PathVariable Long userId,
            @Valid @RequestBody ReassignmentRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearer) {
        access.requireAdmin();
        if (userId <= 0) throw new IllegalArgumentException("User ID must be positive");
        LeaveEmployeeAssociation association = associations.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave association not found"));
        JsonNode user = associationService.validateReferences(userId, request.employeeId(), bearer);
        if (!association.getUsername().equals(user.path("username").asText()))
            throw new LeaveAssociationConflictException("The account no longer matches this association");
        String employeeId = request.employeeId().toString();
        if (!association.getEmployeeId().equals(employeeId) && associations.existsByEmployeeId(employeeId))
            throw new LeaveAssociationConflictException("This employee is already associated");
        if (leaves.existsByEmployeeId(association.getEmployeeId()) || leaves.existsByEmployeeId(employeeId))
            throw new LeaveAssociationConflictException("Reassignment is not allowed while leave requests exist for either employee");
        association.setEmployeeId(employeeId);
        try {
            return associations.saveAndFlush(association);
        } catch (DataIntegrityViolationException ex) {
            throw new LeaveAssociationConflictException("The association conflicts with an existing record");
        }
    }

}
