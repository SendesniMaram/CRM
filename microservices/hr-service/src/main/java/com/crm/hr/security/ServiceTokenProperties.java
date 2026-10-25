package com.crm.hr.security;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Opt-in only. Supply an absolute external PKCS#8 PEM path, never a key value or classpath secret. */
@ConfigurationProperties(prefix = "app.security.employee-service-token", ignoreUnknownFields = false)
public record ServiceTokenProperties(boolean enabled, Path privateKey, String keyId, Duration lifetime) {
    public ServiceTokenProperties {
        if (lifetime == null) lifetime = Duration.ofSeconds(30);
        if (enabled && (privateKey == null || !privateKey.isAbsolute()
                || keyId == null || !keyId.matches("[A-Za-z0-9_-]{1,64}")
                || lifetime.isNegative() || lifetime.isZero() || lifetime.getNano() != 0
                || lifetime.compareTo(Duration.ofSeconds(30)) > 0)) {
            throw new IllegalArgumentException("Invalid HR service token configuration");
        }
    }
}
