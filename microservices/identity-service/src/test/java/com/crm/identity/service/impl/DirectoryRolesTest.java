package com.crm.identity.service.impl;

import com.crm.identity.entity.Role;
import com.crm.identity.entity.User;
import com.crm.identity.enums.RoleType;
import com.crm.identity.repository.RoleRepository;
import com.crm.identity.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DirectoryRolesTest {
    final UserRepository users = mock(UserRepository.class);
    final IdentityServiceImpl service = new IdentityServiceImpl(users, mock(RoleRepository.class), mock(PasswordEncoder.class));

    User account(RoleType... types) {
        User user = new User();
        user.setId(42L); user.setUsername("directory-user"); user.setEmail("directory@example.test");
        user.setPassword("synthetic-password-must-not-appear"); user.setRole(new LinkedHashSet<>());
        for (RoleType type : types) user.getRole().add(new Role(type.name(), type));
        when(users.findById(42L)).thenReturn(Optional.of(user));
        when(users.findAll()).thenReturn(List.of(user));
        return user;
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void completeRolesAreDeterministicAndDeduplicatedForBothEndpoints(boolean reverse) {
        User user = reverse ? account(RoleType.CLIENT, RoleType.EMPLOYEE, RoleType.ADMIN, RoleType.SUPER_ADMIN, RoleType.EMPLOYEE)
                : account(RoleType.EMPLOYEE, RoleType.SUPER_ADMIN, RoleType.ADMIN, RoleType.CLIENT, RoleType.EMPLOYEE);
        var original = List.copyOf(user.getRole());
        for (var response : List.of(service.getUserById(42L), service.getAllUsers().get(0))) {
            assertThat(response.getRoles()).containsExactly("SUPER_ADMIN", "ADMIN", "EMPLOYEE", "CLIENT");
            assertThat(response.getRoleType()).isEqualTo("SUPER_ADMIN");
        }
        assertThat(user.getRole()).containsExactlyElementsOf(original);
        verify(users, never()).save(any());
    }

    @Test void employeeOnlyPreservesLegacyFieldAndJsonContainsNoCredentials() throws Exception {
        account(RoleType.EMPLOYEE);
        var response = service.getUserById(42L);
        var json = new ObjectMapper().valueToTree(response);
        assertThat(response.getRoles()).containsExactly("EMPLOYEE");
        assertThat(json.path("roleType").asText()).isEqualTo("EMPLOYEE");
        assertThat(json.path("id").asLong()).isEqualTo(42);
        assertThat(json.path("enabled").asBoolean()).isTrue();
        assertThat(json.path("accountNonLocked").asBoolean()).isTrue();
        assertThat(json.toString()).doesNotContain("password", "refreshToken", "secret", "synthetic-password");
    }

    @Test void accountWithoutEmployeeDoesNotInventIt() {
        account(RoleType.CLIENT);
        assertThat(service.getUserById(42L).getRoles()).containsExactly("CLIENT");
    }

    @Test void noRolesProducesEmptyCollectionAndNullLegacyRole() {
        account();
        assertThat(service.getUserById(42L).getRoles()).isEmpty();
        assertThat(service.getUserById(42L).getRoleType()).isNull();
    }

    @Test void currentAccountFlagsAreReturnedWithoutMutatingThem() {
        User user = account(RoleType.EMPLOYEE);
        user.setEnabled(false); user.setAccountNonLocked(false);
        var response = service.getUserById(42L);
        assertThat(response.isEnabled()).isFalse();
        assertThat(response.isAccountNonLocked()).isFalse();
    }
}
