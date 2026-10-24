package com.crm.hr.security;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.util.Base64;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ServiceTokenProperties.class)
public class ServiceTokenConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "app.security.employee-service-token", name = "enabled", havingValue = "true")
    public EmployeeServiceTokenSigner employeeServiceTokenSigner(ServiceTokenProperties properties) {
        try (var input = Files.newInputStream(properties.privateKey())) {
            byte[] bytes = input.readNBytes(16385);
            if (bytes.length > 16384) throw new IllegalArgumentException();
            String pem = new String(bytes, StandardCharsets.US_ASCII).trim();
            if (!pem.startsWith("-----BEGIN PRIVATE KEY-----") || !pem.endsWith("-----END PRIVATE KEY-----")) {
                throw new IllegalArgumentException();
            }
            String encoded = pem.substring(27, pem.length() - 25).replaceAll("\\s", "");
            var key = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded)));
            return new EmployeeServiceTokenSigner(key, properties.keyId(), Clock.systemUTC(), properties.lifetime());
        } catch (Exception ex) {
            // Do not echo configuration values, PEM material or parser diagnostics.
            throw new IllegalStateException("HR service signing key could not be configured");
        }
    }
}
