package com.crm.employee.controller;

import com.crm.employee.dto.EmployeeProfileSummary;
import com.crm.employee.security.AuthenticatedServicePrincipal;
import com.crm.employee.security.ServiceJwtVerifier;
import com.crm.employee.service.EmployeeProfileSummaryService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/employees")
public class InternalEmployeeProfileController {
    private final EmployeeProfileSummaryService profiles;
    public InternalEmployeeProfileController(EmployeeProfileSummaryService profiles) { this.profiles = profiles; }

    @GetMapping("/{employeeId}/profile-summary")
    public ResponseEntity<EmployeeProfileSummary> read(@PathVariable String employeeId, Authentication authentication) {
        // Defense in depth: no user principal or alternate resource may reach the repository.
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedServicePrincipal principal)
                || !"hr-service".equals(principal.serviceId())
                || authentication.getAuthorities().stream().noneMatch(a -> ServiceJwtVerifier.AUTHORITY.equals(a.getAuthority()))) {
            throw new AccessDeniedException("Service access denied");
        }
        long id;
        try {
            if (employeeId == null || !employeeId.matches("[1-9][0-9]{0,18}")) throw new IllegalArgumentException();
            id = Long.parseLong(employeeId);
        } catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Canonical Employee identifier required"); }
        if (!employeeId.equals(principal.employeeId())) throw new AccessDeniedException("Service access denied");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(profiles.read(id));
    }
}
