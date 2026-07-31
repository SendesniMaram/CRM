package com.crm.fees.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8088}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fees Service API")
                        .description("Microservice de gestion des frais - CRM")
                        .version("0.0.1-SNAPSHOT")
                        .contact(new Contact()
                                .name("CRM Team")
                                .email("contact@crm.com"))
                        .license(new License()
                                .name("Projet Académique")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Serveur de développement"),
                        new Server()
                                .url("http://localhost:8080/fees")
                                .description("Via Gateway")
                ));
    }
}
