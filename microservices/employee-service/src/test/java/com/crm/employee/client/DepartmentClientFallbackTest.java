package com.crm.employee.client;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import org.springframework.web.server.ResponseStatusException;
class DepartmentClientFallbackTest {
    @Test void factoryFallbackNeverReturnsNullOnUnavailableDependency() {
        var fallback = new DepartmentClientFallbackFactory().create(new IllegalStateException("synthetic private detail"));
        assertThatThrownBy(() -> fallback.getDepartmentById(7L))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode().value()).isEqualTo(503);
                    assertThat(ex.getReason()).doesNotContain("private detail");
                });
    }
    @Test void feignRedirectsAreDisabled() {
        assertThat(new DepartmentClientConfiguration().departmentRequestOptions().isFollowRedirects()).isFalse();
    }
}
