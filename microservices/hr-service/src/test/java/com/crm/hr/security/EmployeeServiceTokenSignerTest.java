package com.crm.hr.security;

import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class EmployeeServiceTokenSignerTest {
    static KeyPair pair;
    static final Instant NOW = Instant.parse("2026-10-22T12:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    @BeforeAll static void keys() throws Exception { var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); pair = generator.generateKeyPair(); }
    EmployeeServiceTokenSigner signer() { return new EmployeeServiceTokenSigner((RSAPrivateKey) pair.getPrivate(), "test-key", CLOCK, Duration.ofSeconds(30)); }

    @Test void signsExactServiceContractWithAnIndependentAsymmetricKey() {
        String token = signer().signForEmployee("9223372036854775807");
        var jwt = Jwts.parser().verifyWith(pair.getPublic()).clock(() -> Date.from(NOW)).build().parseSignedClaims(token);
        assertThat(jwt.getHeader().getType()).isEqualTo("crm-service+jwt");
        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo("RS256");
        assertThat(jwt.getHeader().getKeyId()).isEqualTo("test-key");
        var claims = jwt.getPayload();
        assertThat(claims.getIssuer()).isEqualTo("urn:crm:service"); assertThat(claims.getSubject()).isEqualTo("hr-service");
        assertThat(claims.getAudience()).containsExactly("employee-service");
        assertThat(claims.get("permission")).isEqualTo("employee:profile-summary");
        assertThat(claims.get("method")).isEqualTo("GET");
        assertThat(claims.get("employee_id")).isEqualTo("9223372036854775807");
        assertThat(claims.getIssuedAt().toInstant()).isEqualTo(NOW);
        assertThat(claims.getNotBefore()).isEqualTo(claims.getIssuedAt());
        assertThat(claims.getExpiration().toInstant()).isEqualTo(NOW.plusSeconds(30));
        assertThat(claims).doesNotContainKeys("roles", "uid", "username", "email", "enabled");
    }
    @Test void eachIssuanceHasADifferentTokenId() {
        var parser = Jwts.parser().verifyWith(pair.getPublic()).clock(() -> Date.from(NOW)).build();
        assertThat(parser.parseSignedClaims(signer().signForEmployee("103")).getPayload().getId())
                .isNotEqualTo(parser.parseSignedClaims(signer().signForEmployee("103")).getPayload().getId());
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"0", "01", "-1", "1.0", "1e2", " 42", "9223372036854775808", "103/other"})
    void rejectsInvalidIdentifiers(String id) { assertThatIllegalArgumentException().isThrownBy(() -> signer().signForEmployee(id)); }
    @ParameterizedTest @ValueSource(longs = {-1, 0, 31, 3600})
    void refusesUnboundedOrInvalidLifetime(long seconds) {
        assertThatIllegalArgumentException().isThrownBy(() -> new EmployeeServiceTokenSigner((RSAPrivateKey) pair.getPrivate(), "test", CLOCK, Duration.ofSeconds(seconds)));
    }
    @Test void refusesFractionalLifetime() {
        assertThatIllegalArgumentException().isThrownBy(() -> new EmployeeServiceTokenSigner((RSAPrivateKey) pair.getPrivate(), "test", CLOCK, Duration.ofMillis(1500)));
    }
    @Test void refusesMissingKeyOrInvalidKeyId() {
        assertThatIllegalArgumentException().isThrownBy(() -> new EmployeeServiceTokenSigner(null, "test", CLOCK, Duration.ofSeconds(30)));
        assertThatIllegalArgumentException().isThrownBy(() -> new EmployeeServiceTokenSigner((RSAPrivateKey) pair.getPrivate(), "../key", CLOCK, Duration.ofSeconds(30)));
    }
    @Test void refusesWeakRsaPrivateKey() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(1024);
        var weak = (RSAPrivateKey) generator.generateKeyPair().getPrivate();
        assertThatIllegalArgumentException().isThrownBy(() -> new EmployeeServiceTokenSigner(weak, "test", CLOCK, Duration.ofSeconds(30)));
    }
}
