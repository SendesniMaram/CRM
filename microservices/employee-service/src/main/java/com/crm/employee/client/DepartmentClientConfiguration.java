package com.crm.employee.client;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// Registered only by DepartmentClient, not by application component scanning.
public class DepartmentClientConfiguration {

    @Bean
    public feign.Request.Options departmentRequestOptions() {
        var defaults = new feign.Request.Options();
        return new feign.Request.Options(defaults.connectTimeoutMillis(), java.util.concurrent.TimeUnit.MILLISECONDS,
                defaults.readTimeoutMillis(), java.util.concurrent.TimeUnit.MILLISECONDS, false);
    }

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
