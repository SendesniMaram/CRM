package com.crm.hr.client;

import com.crm.hr.security.EmployeeServiceTokenSigner;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EmployeeProfileClientProperties.class)
public class EmployeeProfileClientConfiguration {
    @Bean(destroyMethod = "close")
    public EmployeeProfileClient employeeProfileClient(EmployeeProfileClientProperties properties, ObjectProvider<EmployeeServiceTokenSigner> signers) {
        if (!properties.enabled()) return new EmployeeProfileClient(properties, null, null, null);
        var signer = signers.getIfAvailable();
        if (signer == null) throw new IllegalStateException("Secure Employee profile client requires the dedicated service signer");
        return create(properties, signer, trust(properties));
    }

    static SSLContext trust(EmployeeProfileClientProperties properties) {
        if (properties.trustedCertificate() == null) return SSLContexts.createSystemDefault();
        try (var input = java.nio.file.Files.newInputStream(properties.trustedCertificate())) {
            byte[] bytes = input.readNBytes(16385);
            if (bytes.length > 16384) throw new IllegalArgumentException();
            var certificate = (java.security.cert.X509Certificate) java.security.cert.CertificateFactory.getInstance("X.509")
                    .generateCertificate(new java.io.ByteArrayInputStream(bytes));
            certificate.checkValidity();
            var store = java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType());
            store.load(null, null);
            store.setCertificateEntry("approved-employee-tls", certificate);
            return SSLContexts.custom().loadTrustMaterial(store, null).build();
        } catch (Exception ex) {
            throw new IllegalStateException("Employee TLS trust could not be configured");
        }
    }

    // Package-private seam for tests trusting only their ephemeral certificate. Hostname verification is never replaced.
    static EmployeeProfileClient create(EmployeeProfileClientProperties properties, EmployeeServiceTokenSigner signer, SSLContext tls) {
        var connections = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create().setSslContext(tls)
                        .setHostnameVerifier(new DefaultHostnameVerifier()).build())
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(properties.connectTimeout().toMillis()))
                        .setSocketTimeout(Timeout.ofMilliseconds(properties.readTimeout().toMillis())).build())
                .setMaxConnTotal(4).setMaxConnPerRoute(4).build();
        var http = HttpClients.custom().setConnectionManager(connections).disableAutomaticRetries().disableRedirectHandling()
                .disableCookieManagement().setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.ofMilliseconds(properties.connectTimeout().toMillis()))
                        .setResponseTimeout(Timeout.ofMilliseconds(properties.readTimeout().toMillis())).build()).build();
        EmployeeProfileClient.Exchange exchange = (id, token) -> {
            var request = new HttpGet(properties.baseUrl().resolve("/api/internal/employees/" + id + "/profile-summary"));
            request.setHeader("Authorization", "Bearer " + token); request.setHeader("Accept", "application/json");
            request.setHeader("Cache-Control", "no-store");
            return http.execute(request, response -> {
                byte[] bytes = new byte[0];
                if (response.getCode() == 200 && response.getEntity() != null) {
                    try (var input = response.getEntity().getContent()) { bytes = input.readNBytes(16385); }
                }
                var type = response.getFirstHeader("Content-Type");
                return new EmployeeProfileClient.Reply(response.getCode(), bytes, type == null ? null : type.getValue());
            });
        };
        return new EmployeeProfileClient(properties, signer, exchange, http);
    }
}
