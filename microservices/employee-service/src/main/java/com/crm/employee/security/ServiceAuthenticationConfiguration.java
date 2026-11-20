package com.crm.employee.security;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ServiceAuthenticationProperties.class)
public class ServiceAuthenticationConfiguration {
    @Bean public ServiceJwtVerifier serviceJwtVerifier(ServiceAuthenticationProperties properties) {
        if (!properties.enabled()) return new ServiceJwtVerifier(Map.of(), Clock.systemUTC());
        var keys = new HashMap<String, RSAPublicKey>();
        try {
            for (var entry : properties.trustedKeys().entrySet()) {
                try (var input = Files.newInputStream(entry.getValue())) {
                    byte[] bytes = input.readNBytes(16385);
                    if (bytes.length > 16384) throw new IllegalArgumentException();
                    String pem = new String(bytes, StandardCharsets.US_ASCII).trim();
                    if (!pem.startsWith("-----BEGIN PUBLIC KEY-----") || !pem.endsWith("-----END PUBLIC KEY-----")) throw new IllegalArgumentException();
                    String encoded = pem.substring(26, pem.length() - 24).replaceAll("\\s", "");
                    keys.put(entry.getKey(), (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                            new X509EncodedKeySpec(Base64.getDecoder().decode(encoded))));
                }
            }
            return new ServiceJwtVerifier(keys, Clock.systemUTC());
        } catch (Exception ex) { throw new IllegalStateException("HR service verification keys could not be configured"); }
    }
}
