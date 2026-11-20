package com.crm.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.gateway.discovery.locator.enabled=false"
})
class GatewayServiceSmokeTest {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void exposesAllHrResourceRoutesThroughHrService() {
        Map<String, RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                .filter(route -> route.getId().startsWith("hr-"))
                .collectMap(RouteDefinition::getId, Function.identity())
                .block();

        assertThat(routes).containsKeys("hr-service", "hr-leaves", "hr-attendances", "hr-trainings", "hr-workflows");
        assertHrRoute(routes.get("hr-service"), "/hr/api/hr-records/**");
        assertHrRoute(routes.get("hr-leaves"), "/hr/api/leaves/**");
        assertHrRoute(routes.get("hr-attendances"), "/hr/api/attendances/**");
        assertHrRoute(routes.get("hr-trainings"), "/hr/api/trainings/**");
        assertHrRoute(routes.get("hr-workflows"), "/hr/api/contracts/**", "/hr/api/evaluations/**",
                "/hr/api/job-offers/**", "/hr/api/applications/**", "/hr/api/employees/*/documents/**",
                "/hr/api/employee-associations/**",
                "/hr/api/employees/*/career-events/**", "/hr/api/employees/*/leave-quotas/**",
                "/hr/api/documents/**", "/hr/api/career-events/**", "/hr/api/leave-quotas/**");
    }

    @Test
    void exposesCrmOpportunitiesAndQuotesRoutes() {
        Map<String, RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                .collectMap(RouteDefinition::getId, Function.identity()).block();
        assertDomainRoute(routes.get("customer-opportunities"), "lb://CUSTOMER-SERVICE", "/customer/api/opportunities/**");
        assertDomainRoute(routes.get("invoice-quotes"), "lb://INVOICE-SERVICE", "/invoice/api/quotes/**");
    }

    private void assertHrRoute(RouteDefinition route, String... expectedPaths) {
        assertThat(route.getUri().toString()).isEqualTo("lb://HR-SERVICE");
        var pathArgs = route.getPredicates().stream().filter(p -> p.getName().equals("Path"))
                .flatMap(p -> p.getArgs().values().stream()).toList();
        assertThat(pathArgs).contains(expectedPaths);
        assertThat(route.getFilters()).anySatisfy(filter -> {
            assertThat(filter.getName()).isEqualTo("StripPrefix");
            assertThat(filter.getArgs()).containsValue("1");
        });
    }

    private void assertDomainRoute(RouteDefinition route, String expectedUri, String expectedPath) {
        assertThat(route.getUri().toString()).isEqualTo(expectedUri);
        assertThat(route.getPredicates()).anySatisfy(predicate ->
                assertThat(predicate.getArgs()).containsValue(expectedPath));
        assertThat(route.getFilters()).anySatisfy(filter -> {
            assertThat(filter.getName()).isEqualTo("StripPrefix");
            assertThat(filter.getArgs()).containsValue("1");
        });
    }
}

