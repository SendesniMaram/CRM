package com.crm.hr.service;

import com.crm.hr.repository.LeaveEmployeeAssociationRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class LeaveAccess {
    private final LeaveEmployeeAssociationRepository associations;
    public LeaveAccess(LeaveEmployeeAssociationRepository associations) { this.associations = associations; }
    public Authentication authentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser"))
            throw new AccessDeniedException("Authentication required");
        return auth;
    }
    public boolean isAdmin() {
        return authentication().getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }
    public void requireAdmin() {
        if (!isAdmin()) throw new AccessDeniedException("Administrative access required");
    }
    public String ownEmployeeId() {
        Authentication auth = authentication();
        if (auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE")))
            throw new AccessDeniedException("Employee access required");
        return associations.findById(auth.getName()).orElseThrow(() ->
                new AccessDeniedException("Employee account is not associated for leave requests")).getEmployeeId();
    }
    public void checkEmployee(String employeeId) {
        if (!isAdmin() && !ownEmployeeId().equals(employeeId))
            throw new AccessDeniedException("This leave request is not accessible");
    }
}
