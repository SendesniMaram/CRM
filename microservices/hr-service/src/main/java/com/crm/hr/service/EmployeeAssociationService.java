package com.crm.hr.service;

import com.crm.hr.client.DirectoryAccount;
import com.crm.hr.client.LeaveDirectoryClient;
import com.crm.hr.entity.LeaveEmployeeAssociation;
import com.crm.hr.exception.LeaveAssociationConflictException;
import com.crm.hr.exception.ResourceNotFoundException;
import com.crm.hr.repository.LeaveEmployeeAssociationRepository;
import com.crm.security.AuthenticatedUserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Single association policy over the existing HR table; no cross-database writes. */
@Service
public class EmployeeAssociationService {
    private final LeaveEmployeeAssociationRepository associations;
    private final LeaveDirectoryClient directory;
    private final TransactionTemplate writes;

    public EmployeeAssociationService(LeaveEmployeeAssociationRepository associations,
            LeaveDirectoryClient directory, PlatformTransactionManager transactions) {
        this.associations = associations;
        this.directory = directory;
        this.writes = new TransactionTemplate(transactions);
    }

    public record Creation(LeaveEmployeeAssociation association, boolean created) {}

    /** Validate both references before opening the write transaction. */
    public Creation create(Long userId, Long employeeId, String bearer) {
        HrAuthorization.requireAdmin();
        JsonNode user = validateReferences(userId, employeeId, bearer);
        String username = user.path("username").textValue();
        try {
            return writes.execute(transaction -> {
                var existing = associations.findByUserId(userId);
                if (existing.isPresent()) {
                    var link = existing.get();
                    if (!link.getEmployeeId().equals(employeeId.toString()) || !link.getUsername().equals(username)) {
                        throw conflict();
                    }
                    return new Creation(link, false);
                }
                // username remains the physical PK, never a fallback for a different User id.
                if (associations.existsById(username) || associations.existsByEmployeeId(employeeId.toString())) {
                    throw conflict();
                }
                var link = new LeaveEmployeeAssociation();
                link.setUserId(userId);
                link.setUsername(username);
                link.setEmployeeId(employeeId.toString());
                return new Creation(associations.saveAndFlush(link), true);
            });
        } catch (DataIntegrityViolationException | ConcurrencyFailureException ex) {
            // Catch outside the write transaction, after rollback. Never return a partial success.
            throw conflict();
        }
    }

    @Transactional(readOnly = true)
    public List<LeaveEmployeeAssociation> list() {
        HrAuthorization.requireAdmin();
        return associations.findAll();
    }

    @Transactional(readOnly = true)
    public LeaveEmployeeAssociation readForAdministration(Long userId) {
        HrAuthorization.requireAdmin();
        requirePositive(userId);
        return find(userId);
    }

    @Transactional(readOnly = true)
    public LeaveEmployeeAssociation readOwn(Authentication authentication) {
        long userId = AuthenticatedUserPrincipal.require(authentication).userId();
        if (authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
            throw new AccessDeniedException("Employee access required");
        }
        return find(userId);
    }

    private LeaveEmployeeAssociation find(long userId) {
        var association = associations.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee association not found"));
        if (association.getUserId() == null || association.getUserId() != userId) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid association identity");
        }
        // A malformed historical reference must not be returned as a usable identity.
        try {
            parseId(association.getEmployeeId());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid association reference");
        }
        return association;
    }

    /** Shared also by legacy reassignment, without changing its history restrictions. */
    public JsonNode validateReferences(Long userId, Long employeeId, String bearer) {
        HrAuthorization.requireAdmin();
        requirePositive(userId);
        requirePositive(employeeId);
        JsonNode user = directory.read("identity-service", "/api/users/" + userId, bearer);
        boolean eligible = DirectoryAccount.eligibleEmployee(user);
        requireRemoteId(user, userId);
        if (!user.path("username").isTextual()) throw invalidDirectory();
        if (!eligible || user.path("username").textValue().isBlank()
                || user.path("username").textValue().length() > 100) {
            throw new IllegalArgumentException("An active employee account is required");
        }
        JsonNode employee = directory.read("employee-service", "/api/employees/" + employeeId, bearer);
        requireRemoteId(employee, employeeId);
        return user;
    }

    private static void requireRemoteId(JsonNode reference, long expected) {
        if (reference == null || !reference.isObject() || !reference.path("id").isIntegralNumber()
                || !reference.path("id").canConvertToLong() || reference.path("id").longValue() <= 0) {
            throw invalidDirectory();
        }
        if (reference.path("id").longValue() != expected) {
            throw new IllegalArgumentException("Directory reference does not match the requested identifier");
        }
    }

    public static long parseId(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException("A canonical positive identifier is required");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Identifier is outside the supported range");
        }
    }

    private static void requirePositive(Long value) {
        if (value == null || value <= 0) throw new IllegalArgumentException("A positive identifier is required");
    }

    private static ResponseStatusException invalidDirectory() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid directory response");
    }

    private static LeaveAssociationConflictException conflict() {
        return new LeaveAssociationConflictException("The association conflicts with an existing record");
    }
}
