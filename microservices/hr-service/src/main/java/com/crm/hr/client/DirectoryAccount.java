package com.crm.hr.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Directory data is not an authentication authority. Never fall back to legacy roleType. */
public final class DirectoryAccount {
    private static final Set<String> KNOWN_ROLES = Set.of("SUPER_ADMIN", "ADMIN", "EMPLOYEE", "CLIENT");
    private DirectoryAccount() {}

    public static boolean eligibleEmployee(JsonNode user) {
        if (user == null || !user.isObject() || !user.path("roles").isArray()
                || !user.path("enabled").isBoolean() || !user.path("accountNonLocked").isBoolean()) {
            throw invalidResponse();
        }
        boolean employee = false;
        for (JsonNode role : user.path("roles")) {
            if (!role.isTextual() || !KNOWN_ROLES.contains(role.textValue())) throw invalidResponse();
            employee |= "EMPLOYEE".equals(role.textValue());
        }
        return employee && user.path("enabled").booleanValue() && user.path("accountNonLocked").booleanValue();
    }

    public static void requireArray(JsonNode response) {
        if (response == null || !response.isArray()) throw invalidResponse();
    }

    private static ResponseStatusException invalidResponse() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Leave directory response invalid");
    }
}
