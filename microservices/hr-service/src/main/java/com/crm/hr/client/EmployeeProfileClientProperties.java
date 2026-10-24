package com.crm.hr.client;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.employee-profile-client", ignoreUnknownFields = false)
public record EmployeeProfileClientProperties(boolean enabled, URI baseUrl, Duration connectTimeout,
        Duration readTimeout, Duration totalTimeout, java.nio.file.Path trustedCertificate) {
    public EmployeeProfileClientProperties(boolean enabled, URI baseUrl, Duration connectTimeout,
            Duration readTimeout, Duration totalTimeout) {
        this(enabled, baseUrl, connectTimeout, readTimeout, totalTimeout, null);
    }
    @org.springframework.boot.context.properties.bind.ConstructorBinding
    public EmployeeProfileClientProperties {
        connectTimeout = connectTimeout == null ? Duration.ofMillis(500) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(1) : readTimeout;
        totalTimeout = totalTimeout == null ? Duration.ofMillis(1500) : totalTimeout;
        if (!bounded(connectTimeout, 5000) || !bounded(readTimeout, 10000) || !bounded(totalTimeout, 15000)
                || totalTimeout.compareTo(connectTimeout) < 0 || totalTimeout.compareTo(readTimeout) < 0
                || enabled && (trustedCertificate != null && (!trustedCertificate.isAbsolute()
                    || !java.nio.file.Files.isRegularFile(trustedCertificate) || !java.nio.file.Files.isReadable(trustedCertificate))
                    || baseUrl == null || !"https".equals(baseUrl.getScheme()) || baseUrl.getHost() == null
                    || baseUrl.getUserInfo() != null || baseUrl.getRawQuery() != null || baseUrl.getRawFragment() != null
                    || !(baseUrl.getRawPath().isEmpty() || baseUrl.getRawPath().equals("/"))
                    || baseUrl.getPort() == 0 || baseUrl.getPort() > 65535 || baseUrl.getPort() < -1)) {
            throw new IllegalArgumentException("Invalid secure Employee profile client configuration");
        }
    }
    private static boolean bounded(Duration value, long maximum) {
        return value.compareTo(Duration.ofMillis(1)) >= 0 && value.compareTo(Duration.ofMillis(maximum)) <= 0;
    }
}
