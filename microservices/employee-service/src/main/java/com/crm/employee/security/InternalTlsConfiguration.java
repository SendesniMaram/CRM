package com.crm.employee.security;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Optional second connector: preserve HTTP discovery while enforcing real TLS internally. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(InternalTlsConfiguration.Properties.class)
public class InternalTlsConfiguration {
    @ConfigurationProperties(prefix = "app.security.internal-tls", ignoreUnknownFields = false)
    public record Properties(boolean enabled, Integer port, Path keyStore, Path passwordFile, String keyAlias) {
        public Properties {
            port = port == null ? 8443 : port;
            keyAlias = keyAlias == null ? "employee" : keyAlias;
            if (enabled && (port < 1024 || port > 65535 || keyAlias.isBlank()
                    || !readable(keyStore) || !readable(passwordFile))) {
                throw new IllegalArgumentException("Invalid internal TLS configuration");
            }
        }
        private static boolean readable(Path path) {
            return path != null && path.isAbsolute() && Files.isRegularFile(path) && Files.isReadable(path);
        }
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> internalTlsConnector(Properties properties) {
        return factory -> {
            if (!properties.enabled()) return;
            if (factory.getPort() == properties.port()) throw new IllegalStateException("Internal TLS requires a distinct port");
            var connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            connector.setPort(properties.port());
            connector.setScheme("https");
            connector.setSecure(true);
            connector.setProperty("SSLEnabled", "true");
            var host = new SSLHostConfig();
            host.setProtocols("TLSv1.2,TLSv1.3");
            var certificate = new SSLHostConfigCertificate(host, SSLHostConfigCertificate.Type.RSA);
            certificate.setCertificateKeystoreType("PKCS12");
            certificate.setCertificateKeystoreFile(properties.keyStore().toString());
            certificate.setCertificateKeystorePasswordFile(properties.passwordFile().toString());
            certificate.setCertificateKeyAlias(properties.keyAlias());
            host.addCertificate(certificate);
            connector.addSslHostConfig(host);
            factory.addAdditionalTomcatConnectors(connector);
        };
    }
}
