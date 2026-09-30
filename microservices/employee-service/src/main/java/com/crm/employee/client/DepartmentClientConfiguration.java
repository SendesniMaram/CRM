package com.crm.employee.client;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// Registered only by DepartmentClient, not by application component scanning.
public class DepartmentClientConfiguration {

    @Bean
    public RequestInterceptor departmentAuthorizationInterceptor() {
        return template -> {
            // The current MVC -> service -> Feign call runs on the request thread.
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                String authorization = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (authorization != null) {
                    template.header(HttpHeaders.AUTHORIZATION, authorization);
                }
            }
        };
    }
}
