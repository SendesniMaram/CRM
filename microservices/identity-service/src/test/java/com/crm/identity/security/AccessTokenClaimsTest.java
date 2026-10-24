package com.crm.identity.security;

import com.crm.identity.exception.InvalidCredentialsException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.*;

class AccessTokenClaimsTest {
    static final String KEY = "synthetic-lot-a-claims-key-at-least-32-bytes";
    private final JwtService jwt = new JwtService(KEY);

    static Map<String, Object> valid() {
        return new HashMap<>(Map.of("sub", "employee", "uid", "101", "enabled", true,
                "roles", List.of("ROLE_EMPLOYEE", "EMPLOYEE"),
                "exp", new Date(System.currentTimeMillis() + 60000)));
    }

    static String sign(Map<String, Object> claims, String key) {
        return Jwts.builder().claims(claims)
                .signWith(Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8))).compact();
    }

    @Test void parsesStableIdentityAndNormalizesOnlyKnownRoles() {
        var claims = jwt.parseAccessToken(sign(valid(), KEY));
        assertThat(claims.userId()).isEqualTo(101);
        assertThat(claims.username()).isEqualTo("employee");
        assertThat(claims.roles()).containsExactly("EMPLOYEE");
    }

    static Stream<Map<String, Object>> invalidClaims() {
        Stream.Builder<Map<String, Object>> cases = Stream.builder();
        for (String field : List.of("uid", "sub", "enabled", "roles", "exp")) {
            var claims = valid(); claims.remove(field); cases.add(claims);
        }
        for (Object value : List.of("0", "-1", "01", " 1", "1.0", "9223372036854775808", 101L)) {
            var claims = valid(); claims.put("uid", value); cases.add(claims);
        }
        for (Object value : List.of(false, "true")) {
            var claims = valid(); claims.put("enabled", value); cases.add(claims);
        }
        for (Object value : List.of(List.of(), List.of("UNKNOWN"), List.of(12), "ADMIN", List.of("ROLE_ROLE_ADMIN"))) {
            var claims = valid(); claims.put("roles", value); cases.add(claims);
        }
        var blank = valid(); blank.put("sub", " "); cases.add(blank);
        var expired = valid(); expired.put("exp", new Date(System.currentTimeMillis() - 1000)); cases.add(expired);
        return cases.build();
    }

    @ParameterizedTest @MethodSource("invalidClaims")
    void rejectsIncompleteOrIncoherentClaims(Map<String, Object> claims) {
        assertThatThrownBy(() -> jwt.parseAccessToken(sign(claims, KEY)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test void rejectsMalformedOrForeignSignature() {
        assertThatThrownBy(() -> jwt.parseAccessToken("malformed"))
                .isInstanceOf(InvalidCredentialsException.class);
        String foreign = sign(valid(), "different-synthetic-lot-a-key-at-least-32-bytes");
        assertThatThrownBy(() -> jwt.parseAccessToken(foreign))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
