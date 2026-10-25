package com.crm.gateway;

import static org.assertj.core.api.Assertions.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.client.enabled=false", "spring.cloud.service-registry.auto-registration.enabled=false",
        "spring.cloud.gateway.discovery.locator.enabled=true", "spring.cloud.loadbalancer.cache.enabled=false"
})
@Import(GatewaySecurityRoutingTest.SyntheticDirectory.class)
class GatewaySecurityRoutingTest {
    private static final AtomicInteger hits = new AtomicInteger();
    private static final AtomicReference<String> bearer = new AtomicReference<>();
    private static final AtomicReference<String> downstreamPath = new AtomicReference<>();
    private static final HttpServer BACKEND = backend();
    @LocalServerPort int port;
    @Autowired RouteDefinitionLocator definitions;
    WebTestClient client;

    static HttpServer backend() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", request -> {
                hits.incrementAndGet();
                bearer.set(request.getRequestHeaders().getFirst("Authorization"));
                String path = request.getRequestURI().getPath();
                downstreamPath.set(path);
                int status = path.contains("/status/") ? Integer.parseInt(path.substring(path.lastIndexOf('/') + 1)) : 200;
                byte[] body = ("synthetic-" + status).getBytes(StandardCharsets.UTF_8);
                request.getResponseHeaders().set("Content-Type", "text/plain");
                request.sendResponseHeaders(status, body.length);
                request.getResponseBody().write(body); request.close();
            });
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
            return server;
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    @BeforeEach void setup() {
        hits.set(0); bearer.set(null); downstreamPath.set(null);
        client = WebTestClient.bindToServer().baseUrl("http://127.0.0.1:" + port)
                .responseTimeout(Duration.ofSeconds(8)).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/internal/access-token/validate", "/identity/api/internal/access-token/validate",
            "/identity-service/api/internal/access-token/validate", "/IDENTITY-SERVICE/api/internal/access-token/validate",
            "/identity-service/api//internal/access-token/validate", "/identity-service/api/./internal/access-token/validate",
            "/identity-service/api/private/../internal/access-token/validate",
            "/identity-service/api;v=1/internal/access-token/validate",
            "/identity-service/%61pi/internal/access-token/validate", "/identity-service/api/%69nternal/access-token/validate",
            "/identity-service/api%2Finternal/access-token/validate", "/identity-service/api%252Finternal/access-token/validate",
            "/identity-service/api%25252Finternal/access-token/validate", "/identity-service/api%5Cinternal/access-token/validate",
            "/identity/api/auth/../internal/access-token/validate"})
    void noIncomingVariantReachesIdentity(String path) {
        client.get().uri(URI.create("http://127.0.0.1:" + port + path)).header("Authorization", "Bearer synthetic")
                .exchange().expectStatus().is4xxClientError();
        assertThat(hits).hasValue(0);
    }

    @ParameterizedTest @ValueSource(strings = {"/rewrite-internal", "/rewrite-encoded"})
    void rewritingPublicAliasToInternalPathIsBlockedBeforeProxying(String alias) {
        client.get().uri(alias).exchange().expectStatus().isNotFound();
        assertThat(hits).hasValue(0);
    }

    @ParameterizedTest @ValueSource(strings = {"GET", "POST", "HEAD", "OPTIONS", "DELETE"})
    void noHttpMethodCanExposeInternalValidation(String method) {
        client.method(org.springframework.http.HttpMethod.valueOf(method))
                .uri("/identity-service/api/internal/access-token/validate").exchange().expectStatus().isNotFound();
        assertThat(hits).hasValue(0);
    }

    @ParameterizedTest @ValueSource(strings = {"%25ZZ", "%00", "%3f", "%25252525252525252525252f", "%C0%AF"})
    void malformedAndAmbiguousEncodingCannotReachBackend(String encoded) {
        client.get().uri(URI.create("http://127.0.0.1:" + port + "/identity-service/api/" + encoded + "/users"))
                .exchange().expectStatus().isBadRequest();
        assertThat(hits).hasValue(0);
    }

    @ParameterizedTest @ValueSource(strings = {"login", "refresh"})
    void legitimateExplicitAuthenticationRoutesArePreserved(String operation) {
        client.post().uri("/identity/api/auth/" + operation).header("Authorization", "Bearer original")
                .bodyValue("synthetic-request").exchange().expectStatus().isOk();
        assertThat(downstreamPath.get()).isEqualTo("/api/auth/" + operation);
        assertThat(bearer.get()).isEqualTo("Bearer original");
        assertThat(hits).hasValue(1);
    }

    @Test void actualDiscoveryLocatorStillRoutesLegitimateLogin() {
        var routes = definitions.getRouteDefinitions().collectList().block(Duration.ofSeconds(5));
        assertThat(routes).anySatisfy(route -> {
            assertThat(route.getId()).contains("IDENTITY-SERVICE");
            assertThat(route.getPredicates()).anySatisfy(predicate ->
                    assertThat(predicate.getArgs()).containsValue("/identity-service/**"));
        });
        client.post().uri("/identity-service/api/auth/login").exchange().expectStatus().isOk();
        assertThat(downstreamPath.get()).isEqualTo("/api/auth/login");
        assertThat(hits).hasValue(1);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"identity,401", "identity,403", "identity,503",
            "identity-service,401", "identity-service,403", "identity-service,503"})
    void downstreamErrorStatusAndBodyAreNotRewritten(String prefix, int status) {
        client.get().uri("/" + prefix + "/api/users/status/" + status).header("Authorization", "Bearer unchanged")
                .exchange().expectStatus().isEqualTo(status).expectBody(String.class).isEqualTo("synthetic-" + status);
        assertThat(bearer.get()).isEqualTo("Bearer unchanged");
        assertThat(hits).hasValue(1);
    }

    @Test void internalTextInQueryDoesNotBlockPublicAuthentication() {
        client.post().uri("/identity/api/auth/login?return=/api/internal/access-token/validate")
                .exchange().expectStatus().isOk();
        assertThat(hits).hasValue(1);
    }

    @Test void legitimateLoginCorsPreflightIsPreserved() {
        client.options().uri("/identity/api/auth/login").header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization")
                .exchange().expectStatus().isOk().expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:4200");
        assertThat(hits).hasValue(0);
    }

    @Test void explicitPersonalProfileRouteRewritesToHrWithoutChangingBearer() {
        client.get().uri("/hr/api/employee-profile/me").header("Authorization", "Bearer original-user")
                .exchange().expectStatus().isOk();
        assertThat(downstreamPath.get()).isEqualTo("/api/employee-profile/me");
        assertThat(bearer.get()).isEqualTo("Bearer original-user"); assertThat(hits).hasValue(1);
    }
    @ParameterizedTest @ValueSource(strings = {"/employee/api/internal/employees/103/profile-summary",
            "/employee-service/api/internal/employees/103/profile-summary", "/employee-service/api%2Finternal/employees/103/profile-summary",
            "/employee-service/api%252Finternal/employees/103/profile-summary"})
    void internalEmployeeProjectionIsNeverPublic(String path) {
        client.get().uri(URI.create("http://127.0.0.1:" + port + path)).exchange().expectStatus().is4xxClientError();
        assertThat(hits).hasValue(0);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SyntheticDirectory {
        @Bean ReactiveDiscoveryClient syntheticDiscoveryClient() {
            return new ReactiveDiscoveryClient() {
                public String description() { return "Isolated synthetic directory"; }
                public Flux<String> getServices() { return Flux.just("IDENTITY-SERVICE", "HR-SERVICE", "EMPLOYEE-SERVICE"); }
                public Flux<ServiceInstance> getInstances(String serviceId) {
                    return java.util.Set.of("identity-service", "hr-service", "employee-service").contains(serviceId.toLowerCase(java.util.Locale.ROOT))
                            ? Flux.just(new DefaultServiceInstance("synthetic", serviceId.toUpperCase(java.util.Locale.ROOT), "127.0.0.1", BACKEND.getAddress().getPort(), false))
                            : Flux.empty();
                }
            };
        }
        @Bean RouteLocator syntheticRewriteRoute(RouteLocatorBuilder builder) {
            return builder.routes().route("synthetic-internal-rewrite", route -> route.path("/rewrite-internal")
                    .filters(filters -> filters.rewritePath("/rewrite-internal", "/api/internal/access-token/validate"))
                    .uri("http://127.0.0.1:" + BACKEND.getAddress().getPort()))
                    .route("synthetic-encoded-rewrite", route -> route.path("/rewrite-encoded")
                    .filters(filters -> filters.rewritePath("/rewrite-encoded", "/api%2Finternal/access-token/validate"))
                    .uri("http://127.0.0.1:" + BACKEND.getAddress().getPort())).build();
        }
    }
}
