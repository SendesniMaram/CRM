package com.crm.employee.security;

import java.security.Principal;
import java.time.Instant;

/** Distinct from AuthenticatedUserPrincipal; never carries application roles. */
public record AuthenticatedServicePrincipal(String serviceId, String employeeId, String tokenId,
        Instant expiresAt) implements Principal {
    @Override public String getName() { return serviceId; }
}
