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
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(
        print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
class DepartmentClientAuthorizationTest extends com.crm.employee.security.TestIdentitySupport {

    @Autowired
    private DepartmentClient departmentClient;

    @Autowired
    private CapturingClient transport;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mvc;

    @Autowired
    private com.crm.security.JwtService jwt;

    @Autowired
    private com.crm.employee.repository.EmployeeRepository employees;

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"ADMIN", "SUPER_ADMIN"})
    @org.springframework.transaction.annotation.Transactional
    void administrativeDetailsStillForwardJwtToDepartment(String role) throws Exception {
        var employee = new com.crm.employee.entity.Employee();
        employee.setEmployeeCode("SECURITY-" + role);
        employee.setSalary(java.math.BigDecimal.ZERO);
        employee.setDepartmentId(7L);
        Long id = employees.saveAndFlush(employee).getId();
        String bearer = "Bearer " + com.crm.employee.security.TestIdentitySupport.token("test-admin", "", true, List.of(role));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/employees/" + id + "/details")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.department.id").value(7));

        org.junit.jupiter.api.Assertions.assertTrue(
                transport.request.headers().get(HttpHeaders.AUTHORIZATION).contains(bearer),
                "The administrator authorization must be forwarded to Department");
    }

    @AfterEach
    void clearRequestContext() {
        transport.status = 200;
        RequestContextHolder.resetRequestAttributes();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {401, 403, 503})
    @org.springframework.transaction.annotation.Transactional
    void downstreamSecurityFailureIsNotConvertedToMissingDepartment(int status) throws Exception {
        var employee = new com.crm.employee.entity.Employee();
        employee.setEmployeeCode("DOWNSTREAM-" + status);
        employee.setSalary(java.math.BigDecimal.ZERO);
        employee.setDepartmentId(7L);
        Long id = employees.saveAndFlush(employee).getId();
        transport.status = status;
        String bearer = "Bearer " + com.crm.employee.security.TestIdentitySupport.token("test-admin", "", true, List.of("ADMIN"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/employees/" + id + "/details")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(status));
        assertThat(transport.request.headers().get(HttpHeaders.AUTHORIZATION)).containsExactly(bearer);
        assertThat(employees.findById(id).orElseThrow().getDepartmentId()).isEqualTo(7L);
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
        private int status = 200;
        private Request request;
        private Thread thread;

        @Override
        public Response execute(Request request, Request.Options options) {
            assertThat(options.isFollowRedirects()).isFalse();
            this.request = request;
            this.thread = Thread.currentThread();
            return Response.builder()
                    .request(request)
                    .status(status)
                    .reason("OK")
                    .headers(Map.of(HttpHeaders.CONTENT_TYPE, List.of("application/json")))
                    .body("{\"id\":7,\"name\":\"Test department\"}", StandardCharsets.UTF_8)
                    .build();
        }
    }
}
