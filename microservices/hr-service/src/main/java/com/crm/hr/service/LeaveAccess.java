package com.crm.hr.service;

import com.crm.hr.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LeaveAccess {
    private final EmployeeAssociationService associations;
    public LeaveAccess(EmployeeAssociationService associations) { this.associations = associations; }
    public Authentication authentication() {
        return HrAuthorization.authentication();
    }
    public boolean isAdmin() {
        return HrAuthorization.isAdmin();
    }
    public void requireAdmin() {
        HrAuthorization.requireAdmin();
    }
    public String ownEmployeeId() {
        try {
            // readOwn requires the validated typed principal and resolves exclusively by uid.
            return associations.readOwn(authentication()).getEmployeeId();
        } catch (ResourceNotFoundException ex) {
            throw new AccessDeniedException("Employee account is not associated for leave requests");
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() != 502) throw ex;
            throw new AccessDeniedException("Employee association unavailable for leave access");
        }
    }
    public void checkEmployee(String employeeId) {
        if (!isAdmin() && !ownEmployeeId().equals(employeeId))
            throw new AccessDeniedException("This leave request is not accessible");
    }
}
