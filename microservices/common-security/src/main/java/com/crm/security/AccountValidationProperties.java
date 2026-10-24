package com.crm.security;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** No localhost default or switch disabling the security check. */
@ConfigurationProperties(prefix = "app.security.account-validation", ignoreUnknownFields = false)
public record AccountValidationProperties(URI baseUrl, Duration connectTimeout,
                                          Duration readTimeout, Duration totalTimeout) {
    public AccountValidationProperties {
        if (baseUrl == null || !("http".equals(baseUrl.getScheme()) || "https".equals(baseUrl.getScheme()))
                || baseUrl.getHost() == null || baseUrl.getUserInfo() != null
                || baseUrl.getQuery() != null || baseUrl.getFragment() != null
                || !(baseUrl.getPath().isEmpty() || "/".equals(baseUrl.getPath()))
                || baseUrl.getPort() == 0 || baseUrl.getPort() > 65535) {
            throw new IllegalArgumentException("Account validation requires a trusted HTTP(S) origin");
        }
        connectTimeout = bounded(connectTimeout, Duration.ofMillis(500), 5000);
        readTimeout = bounded(readTimeout, Duration.ofSeconds(1), 10000);
        totalTimeout = bounded(totalTimeout, Duration.ofMillis(1500), 15000);
        if (connectTimeout.compareTo(totalTimeout) > 0 || readTimeout.compareTo(totalTimeout) > 0) {
            throw new IllegalArgumentException("Socket timeouts must not exceed the total timeout");
        }
    }

    private static Duration bounded(Duration value, Duration fallback, long maximumMillis) {
        Duration result = value == null ? fallback : value;
        if (result.compareTo(Duration.ofMillis(1)) < 0
                || result.compareTo(Duration.ofMillis(maximumMillis)) > 0) {
            throw new IllegalArgumentException("Account validation timeout outside allowed bounds");
        }
        return result;
    }
}
