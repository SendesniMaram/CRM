package com.crm.employee.security;

import java.nio.file.Path;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Disabled means the internal namespace stays closed, not that authentication is bypassed. */
@ConfigurationProperties(prefix = "app.security.hr-service-auth", ignoreUnknownFields = false)
public record ServiceAuthenticationProperties(boolean enabled, Map<String, Path> trustedKeys) {
    public ServiceAuthenticationProperties {
        trustedKeys = trustedKeys == null ? Map.of() : Map.copyOf(trustedKeys);
        if (enabled && (trustedKeys.isEmpty() || trustedKeys.size() > 8 || trustedKeys.entrySet().stream().anyMatch(e ->
                !e.getKey().matches("[A-Za-z0-9_-]{1,64}") || e.getValue() == null || !e.getValue().isAbsolute()))) {
            throw new IllegalArgumentException("Invalid HR service verification configuration");
        }
    }
}
