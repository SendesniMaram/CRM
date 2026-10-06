package com.crm.identity.config;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.crm.identity.entity.Role;
import com.crm.identity.entity.User;
import com.crm.identity.enums.RoleType;
import com.crm.identity.repository.RoleRepository;
import com.crm.identity.repository.UserRepository;

/** Explicit local-only provisioning; never updates or promotes an existing account. */
@Component
@Profile({"local", "dev"})
@ConditionalOnProperty(prefix = "app.bootstrap.admin", name = "enabled", havingValue = "true")
@Order(1)
public class DevAdminBootstrap implements CommandLineRunner {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final Environment environment;

    public DevAdminBootstrap(UserRepository users, RoleRepository roles,
                             PasswordEncoder encoder, Environment environment) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Held until commit: concurrent bootstraps must recheck administration after this lock.
        Role role = roles.lockByRoleType(RoleType.SUPER_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Bootstrap requires the initialized SUPER_ADMIN role"));
        if (users.existsByRole_RoleTypeIn(List.of(RoleType.ADMIN, RoleType.SUPER_ADMIN))) return;

        String username = required("username");
        String email = required("email");
        String password = required("password");
        if (username.length() > 100 || !username.equals(username.trim()) || username.contains("@")) {
            throw invalid("username");
        }
        if (email.length() > 100 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw invalid("email");
        }
        if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw invalid("password");
        }
        if (users.existsByUsername(username) || users.existsByEmail(email)) {
            throw new IllegalStateException("Bootstrap account conflicts with an existing account; no account changed");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(encoder.encode(password));
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.getRole().add(role);
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            // Do not propagate database diagnostics that may include account values.
            throw new IllegalStateException("Bootstrap account could not be persisted; no account changed");
        }
    }

    private String required(String name) {
        String value = environment.getProperty("app.bootstrap.admin." + name);
        if (value == null || value.isBlank()) throw invalid(name);
        return value;
    }

    private IllegalStateException invalid(String name) {
        return new IllegalStateException("Missing or invalid configuration: app.bootstrap.admin." + name);
    }
}
