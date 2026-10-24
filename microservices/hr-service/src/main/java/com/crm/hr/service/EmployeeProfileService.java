package com.crm.hr.service;

import com.crm.hr.client.EmployeeProfileClient;
import com.crm.hr.dto.EmployeeProfileSummary;
import com.crm.hr.exception.EmployeeProfileException;
import com.crm.hr.exception.ResourceNotFoundException;
import com.crm.security.AuthenticatedUserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class EmployeeProfileService {
    private final EmployeeAssociationService associations;
    private final EmployeeProfileClient employee;
    public EmployeeProfileService(EmployeeAssociationService associations, EmployeeProfileClient employee) {
        this.associations = associations; this.employee = employee;
    }
    public EmployeeProfileSummary own(Authentication authentication) {
        long uid = AuthenticatedUserPrincipal.require(authentication).userId();
        if (authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) throw new AccessDeniedException("Employee access required");
        com.crm.hr.entity.LeaveEmployeeAssociation link;
        try { link = associations.readOwn(authentication); }
        catch (ResourceNotFoundException ex) { throw new EmployeeProfileException(404, "PROFILE_NOT_ASSOCIATED"); }
        catch (org.springframework.web.server.ResponseStatusException ex) { throw new EmployeeProfileException(502, "PROFILE_INVALID_RESPONSE"); }
        if (link == null || link.getUserId() == null || link.getUserId() != uid) throw invalid();
        try { EmployeeAssociationService.parseId(link.getEmployeeId()); }
        catch (IllegalArgumentException ex) { throw invalid(); }
        return employee.read(link.getEmployeeId());
    }
    private EmployeeProfileException invalid() { return new EmployeeProfileException(502, "PROFILE_INVALID_RESPONSE"); }
}
