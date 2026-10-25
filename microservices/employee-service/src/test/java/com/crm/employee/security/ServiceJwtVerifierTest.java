package com.crm.employee.security;

import io.jsonwebtoken.Jwts;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import static org.assertj.core.api.Assertions.*;

class ServiceJwtVerifierTest {
    static final Instant NOW = Instant.parse("2026-10-22T12:00:00Z");
    final ServiceJwtVerifier verifier = new ServiceJwtVerifier(Map.of("test-key", (RSAPublicKey) ServiceJwtTestTokens.HR.getPublic()), Clock.fixed(NOW, ZoneOffset.UTC));
    String token(Consumer<io.jsonwebtoken.JwtBuilder> change) {
        var builder = ServiceJwtTestTokens.builder(NOW); change.accept(builder);
        return builder.signWith(ServiceJwtTestTokens.HR.getPrivate(), Jwts.SIG.RS256).compact();
    }
    @Test void validTokenCreatesOnlyAServicePrincipal() {
        var principal = verifier.verify(token(b -> {}), "GET", "103");
        assertThat(principal.getName()).isEqualTo("hr-service"); assertThat(principal.employeeId()).isEqualTo("103");
        assertThat(principal).isNotInstanceOf(com.crm.security.AuthenticatedUserPrincipal.class);
        assertThat(principal.expiresAt()).isEqualTo(NOW.plusSeconds(30));
    }
    @Test void canonicalLongIdentifierIsNotRoundedOrReinterpreted() {
        String id = "9223372036854775807";
        assertThat(verifier.verify(token(b -> b.claim("employee_id", id)), "GET", id).employeeId()).isEqualTo(id);
    }
    @Test void expirationBoundaryIsStrictWithoutGracePeriod() {
        var later = new ServiceJwtVerifier(Map.of("test-key", (RSAPublicKey) ServiceJwtTestTokens.HR.getPublic()),
                Clock.fixed(NOW.plusSeconds(30), ZoneOffset.UTC));
        assertThatThrownBy(() -> later.verify(token(b -> {}), "GET", "103")).isInstanceOf(BadCredentialsException.class);
    }
    @Test void weakTrustedRsaKeyIsRefused() throws Exception {
        var generator = java.security.KeyPairGenerator.getInstance("RSA"); generator.initialize(1024);
        var weak = (RSAPublicKey) generator.generateKeyPair().getPublic();
        assertThatIllegalArgumentException().isThrownBy(() -> new ServiceJwtVerifier(Map.of("test-key", weak), Clock.systemUTC()));
    }
    static Stream<Arguments> invalidClaims() {
        return Stream.of(
            Arguments.of("issuer", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.issuer("untrusted")),
            Arguments.of("subject", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.subject("other-service")),
            Arguments.of("audience", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.audience().clear().add("other").and()),
            Arguments.of("extra audience", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.audience().add("other").and()),
            Arguments.of("expired", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.expiration(Date.from(NOW.minusSeconds(1)))),
            Arguments.of("activation", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.notBefore(Date.from(NOW.plusSeconds(1)))),
            Arguments.of("future issuance", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.issuedAt(Date.from(NOW.plusSeconds(1)))),
            Arguments.of("maximum lifetime", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.expiration(Date.from(NOW.plusSeconds(31)))),
            Arguments.of("missing expiry", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.expiration(null)),
            Arguments.of("missing issued", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.issuedAt(null)),
            Arguments.of("missing activation", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.notBefore(null)),
            Arguments.of("type", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.header().type("JWT").and()),
            Arguments.of("kid", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.header().keyId("unknown").and()),
            Arguments.of("missing kid", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.header().keyId(null).and()),
            Arguments.of("jku", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.header().add("jku", "https://untrusted.invalid/keys").and()),
            Arguments.of("x5u", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.header().add("x5u", "https://untrusted.invalid/cert").and()),
            Arguments.of("missing jti", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.id(null)),
            Arguments.of("invalid jti", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.id("invalid")),
            Arguments.of("roles", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.claim("roles", java.util.List.of("ADMIN"))),
            Arguments.of("numeric employee", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.claim("employee_id", 103)),
            Arguments.of("malformed employee", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.claim("employee_id", "0103")),
            Arguments.of("overflow employee", (Consumer<io.jsonwebtoken.JwtBuilder>) b -> b.claim("employee_id", "9223372036854775808")));
    }
    @ParameterizedTest(name = "{0}") @MethodSource("invalidClaims")
    void invalidCredentialsFailClosed(String name, Consumer<io.jsonwebtoken.JwtBuilder> change) {
        assertThatThrownBy(() -> verifier.verify(token(change), "GET", "103")).isInstanceOf(BadCredentialsException.class);
    }
    @Test void wrongSignatureIsRejected() {
        String token = ServiceJwtTestTokens.builder(NOW).signWith(ServiceJwtTestTokens.OTHER.getPrivate(), Jwts.SIG.RS256).compact();
        assertThatThrownBy(() -> verifier.verify(token, "GET", "103")).isInstanceOf(BadCredentialsException.class);
    }
    @Test void unexpectedAlgorithmIsRejectedEvenWithTheTrustedKey() {
        String token = ServiceJwtTestTokens.builder(NOW).signWith(ServiceJwtTestTokens.HR.getPrivate(), Jwts.SIG.RS512).compact();
        assertThatThrownBy(() -> verifier.verify(token, "GET", "103")).isInstanceOf(BadCredentialsException.class);
    }
    @Test void unsignedTokenIsRejected() {
        String token = ServiceJwtTestTokens.builder(NOW).compact();
        assertThatThrownBy(() -> verifier.verify(token, "GET", "103")).isInstanceOf(BadCredentialsException.class);
    }
    @ParameterizedTest @ValueSource(strings = {"POST", "PUT", "DELETE", "HEAD", "OPTIONS"})
    void noOtherMethodIsAuthorized(String method) {
        assertThatThrownBy(() -> verifier.verify(token(b -> {}), method, "103")).isInstanceOf(AccessDeniedException.class);
    }
    @Test void resourceAndPermissionMustMatchExactly() {
        assertThatThrownBy(() -> verifier.verify(token(b -> {}), "GET", "104")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> verifier.verify(token(b -> b.claim("permission", "employee:read")), "GET", "103")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> verifier.verify(token(b -> b.claim("method", "POST")), "GET", "103")).isInstanceOf(AccessDeniedException.class);
    }
    @ParameterizedTest @ValueSource(strings = {"01", "0", "-1", "1.0", "9223372036854775808"})
    void malformedRequestedResourceIsRefused(String id) {
        assertThatIllegalArgumentException().isThrownBy(() -> verifier.verify(token(b -> {}), "GET", id));
    }
    @Test void emptyTrustNeverAcceptsAnySignature() {
        var closed = new ServiceJwtVerifier(Map.of(), Clock.fixed(NOW, ZoneOffset.UTC));
        assertThat(closed.configured()).isFalse();
        assertThatThrownBy(() -> closed.verify(token(b -> {}), "GET", "103")).isInstanceOf(BadCredentialsException.class);
    }
    @Test void rotationAcceptsOnlyExplicitlyTrustedOldAndNewKeyIds() {
        var rotating = new ServiceJwtVerifier(Map.of("test-key", (RSAPublicKey) ServiceJwtTestTokens.HR.getPublic(),
                "next-key", (RSAPublicKey) ServiceJwtTestTokens.OTHER.getPublic()), Clock.fixed(NOW, ZoneOffset.UTC));
        String next = ServiceJwtTestTokens.builder(NOW).header().keyId("next-key").and()
                .signWith(ServiceJwtTestTokens.OTHER.getPrivate(), Jwts.SIG.RS256).compact();
        assertThat(rotating.verify(token(b -> {}), "GET", "103").serviceId()).isEqualTo("hr-service");
        assertThat(rotating.verify(next, "GET", "103").serviceId()).isEqualTo("hr-service");
        assertThatThrownBy(() -> verifier.verify(next, "GET", "103")).isInstanceOf(BadCredentialsException.class);
    }
}
