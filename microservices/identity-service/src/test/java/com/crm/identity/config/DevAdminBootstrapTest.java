package com.crm.identity.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.crm.identity.entity.Role;
import com.crm.identity.entity.User;
import com.crm.identity.enums.RoleType;
import com.crm.identity.repository.RoleRepository;
import com.crm.identity.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DevAdminBootstrapTest {

    @Mock
    private UserRepository users;

    @Mock
    private RoleRepository roles;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private Environment environment;

    @Test
    void bootstrapRequiresLocalOrDevProfile() {
        Profile profile = DevAdminBootstrap.class.getAnnotation(Profile.class);

        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactlyInAnyOrder("local", "dev");
    }

    @Test
    void bootstrapRequiresExplicitEnabledProperty() {
        ConditionalOnProperty condition = DevAdminBootstrap.class.getAnnotation(ConditionalOnProperty.class);

        assertThat(condition).isNotNull();
        assertThat(condition.prefix()).isEqualTo("app.bootstrap.admin");
        assertThat(condition.name()).containsExactly("enabled");
        assertThat(condition.havingValue()).isEqualTo("true");
    }

    @Test
    void enabledBootstrapCreatesSuperAdminWhenNoAdminExists() {
        Role superAdminRole = new Role("SUPER_ADMIN", RoleType.SUPER_ADMIN);
        when(roles.lockByRoleType(RoleType.SUPER_ADMIN)).thenReturn(Optional.of(superAdminRole));
        when(users.existsByRole_RoleTypeIn(List.of(RoleType.ADMIN, RoleType.SUPER_ADMIN))).thenReturn(false);
        when(environment.getProperty("app.bootstrap.admin.username")).thenReturn("bootstrap-admin");
        when(environment.getProperty("app.bootstrap.admin.email")).thenReturn("bootstrap-admin@example.com");
        when(environment.getProperty("app.bootstrap.admin.password")).thenReturn("StrongP@ssw0rd123");
        when(users.existsByUsername("bootstrap-admin")).thenReturn(false);
        when(users.existsByEmail("bootstrap-admin@example.com")).thenReturn(false);
        when(encoder.encode("StrongP@ssw0rd123")).thenReturn("encoded-super-admin-password");

        new DevAdminBootstrap(users, roles, encoder, environment).run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users, times(1)).saveAndFlush(userCaptor.capture());
        User createdUser = userCaptor.getValue();

        assertThat(createdUser.getUsername()).isEqualTo("bootstrap-admin");
        assertThat(createdUser.getEmail()).isEqualTo("bootstrap-admin@example.com");
        assertThat(createdUser.getPassword()).isEqualTo("encoded-super-admin-password");
        assertThat(createdUser.isEnabled()).isTrue();
        assertThat(createdUser.isAccountNonLocked()).isTrue();
        assertThat(createdUser.getRole()).extracting(Role::getRoleType).containsExactly(RoleType.SUPER_ADMIN);
        verify(encoder).encode("StrongP@ssw0rd123");
    }

    @Test
    void existingAdminPreventsBootstrapCreation() {
        Role superAdminRole = new Role("SUPER_ADMIN", RoleType.SUPER_ADMIN);
        when(roles.lockByRoleType(RoleType.SUPER_ADMIN)).thenReturn(Optional.of(superAdminRole));
        when(users.existsByRole_RoleTypeIn(List.of(RoleType.ADMIN, RoleType.SUPER_ADMIN))).thenReturn(true);

        new DevAdminBootstrap(users, roles, encoder, environment).run();

        verify(users, never()).saveAndFlush(any(User.class));
    }

    @Test
    void missingBootstrapConfigFailsClosed() {
        Role superAdminRole = new Role("SUPER_ADMIN", RoleType.SUPER_ADMIN);
        when(roles.lockByRoleType(RoleType.SUPER_ADMIN)).thenReturn(Optional.of(superAdminRole));
        when(users.existsByRole_RoleTypeIn(List.of(RoleType.ADMIN, RoleType.SUPER_ADMIN))).thenReturn(false);
        when(environment.getProperty("app.bootstrap.admin.username")).thenReturn("bootstrap-admin");
        when(environment.getProperty("app.bootstrap.admin.email")).thenReturn("bootstrap-admin@example.com");
        when(environment.getProperty("app.bootstrap.admin.password")).thenReturn(null);

        assertThatThrownBy(() -> new DevAdminBootstrap(users, roles, encoder, environment).run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.bootstrap.admin.password");

        verify(users, never()).saveAndFlush(any(User.class));
    }

    @Test
    void secondExecutionDoesNotCreateDuplicate() {
        Role superAdminRole = new Role("SUPER_ADMIN", RoleType.SUPER_ADMIN);
        when(roles.lockByRoleType(RoleType.SUPER_ADMIN)).thenReturn(Optional.of(superAdminRole));
        when(users.existsByRole_RoleTypeIn(List.of(RoleType.ADMIN, RoleType.SUPER_ADMIN))).thenReturn(false);
        when(environment.getProperty("app.bootstrap.admin.username")).thenReturn("bootstrap-admin");
        when(environment.getProperty("app.bootstrap.admin.email")).thenReturn("bootstrap-admin@example.com");
        when(environment.getProperty("app.bootstrap.admin.password")).thenReturn("StrongP@ssw0rd123");
        when(users.existsByUsername("bootstrap-admin")).thenReturn(false);
        when(users.existsByEmail("bootstrap-admin@example.com")).thenReturn(false);
        when(encoder.encode("StrongP@ssw0rd123")).thenReturn("encoded-super-admin-password");

        DevAdminBootstrap bootstrap = new DevAdminBootstrap(users, roles, encoder, environment);
        bootstrap.run();

        when(users.existsByRole_RoleTypeIn(List.of(RoleType.ADMIN, RoleType.SUPER_ADMIN))).thenReturn(true);
        bootstrap.run();

        verify(users, times(1)).saveAndFlush(any(User.class));
    }
}
