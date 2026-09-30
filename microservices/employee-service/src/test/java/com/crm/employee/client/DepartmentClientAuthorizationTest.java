package com.crm.employee.client;

import feign.Client;
import feign.Request;
import feign.RequestInterceptor;
import feign.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.openfeign.client.config.department-service.url=http://department.test"
})
@Import(DepartmentClientAuthorizationTest.TransportConfiguration.class)
class DepartmentClientAuthorizationTest {

    @Autowired
    private DepartmentClient departmentClient;

    @Autowired
    private CapturingClient transport;

    @Autowired
    private ApplicationContext applicationContext;

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void forwardsBearerHeaderExactlyOnTheRequestThread() {
        bindRequest("Bearer test-token");

        departmentClient.getDepartmentById(7L);

        assertThat(transport.request.headers().get(HttpHeaders.AUTHORIZATION))
                .containsExactly("Bearer test-token");
        assertThat(transport.request.url()).isEqualTo("http://department.test/api/departments/7");
        assertThat(transport.thread).isSameAs(Thread.currentThread());
    }

    @Test
    void doesNotInventAuthorizationWhenHeaderIsAbsent() {
        bindRequest(null);

        departmentClient.getDepartmentById(7L);

        assertThat(transport.request.headers()).doesNotContainKey(HttpHeaders.AUTHORIZATION);
    }

    @Test
    void preservesNonBearerHeaderWithoutConvertingIt() {
        bindRequest("Basic fictitious-credentials");

        departmentClient.getDepartmentById(7L);

        assertThat(transport.request.headers().get(HttpHeaders.AUTHORIZATION))
                .containsExactly("Basic fictitious-credentials");
    }

    @Test
    void doesNotInventAuthorizationOutsideAnHttpRequest() {
        RequestContextHolder.resetRequestAttributes();

        departmentClient.getDepartmentById(7L);

        assertThat(transport.request.headers()).doesNotContainKey(HttpHeaders.AUTHORIZATION);
    }

    @Test
    void doesNotRegisterInterceptorInTheApplicationContext() {
        assertThat(applicationContext.getBeansOfType(RequestInterceptor.class)).isEmpty();
    }

    private void bindRequest(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorization != null) {
            request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TransportConfiguration {
        @Bean
        CapturingClient departmentTestTransport() {
            return new CapturingClient();
        }
    }

    static class CapturingClient implements Client {
        private Request request;
        private Thread thread;

        @Override
        public Response execute(Request request, Request.Options options) {
            this.request = request;
            this.thread = Thread.currentThread();
            return Response.builder()
                    .request(request)
                    .status(200)
                    .reason("OK")
                    .headers(Map.of(HttpHeaders.CONTENT_TYPE, List.of("application/json")))
                    .body("{\"id\":7,\"name\":\"Test department\"}", StandardCharsets.UTF_8)
                    .build();
        }
    }
}
