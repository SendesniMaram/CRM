package com.crm.identity.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.crm.identity.dto.RegisterRequest;
import com.crm.identity.entity.Role;
import com.crm.identity.entity.User;
import com.crm.identity.enums.RoleType;
import com.crm.identity.repository.RefreshTokenRepository;
import com.crm.identity.repository.RoleRepository;
import com.crm.identity.repository.UserRepository;
import com.crm.identity.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private Role clientRole;

    @BeforeEach
    void setUp() {
        clientRole = new Role("CLIENT", RoleType.CLIENT);
        when(roleRepository.findByRoleType(RoleType.CLIENT)).thenReturn(Optional.of(clientRole));
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
    }

    @Test
    void normalRegistrationCreatesClientUser() {
        RegisterRequest request = registerRequest();

        authenticationService.register(request);

        User savedUser = savedUser();
        assertThat(savedUser.getRole()).containsExactly(clientRole);
        assertThat(savedUser.getUsername()).isEqualTo("test-user");
    }

    @Test
    void adminRoleInputCannotCreateAdminUser() throws Exception {
        RegisterRequest request = objectMapper.readValue(registrationJson("ADMIN"), RegisterRequest.class);

        authenticationService.register(request);

        assertThat(savedUser().getRole()).extracting(Role::getRoleType).containsExactly(RoleType.CLIENT);
        verify(roleRepository, never()).findByRoleType(RoleType.ADMIN);
    }

    @Test
    void superAdminRoleInputCannotCreateSuperAdminUser() throws Exception {
        RegisterRequest request = objectMapper.readValue(registrationJson("SUPER_ADMIN"), RegisterRequest.class);

        authenticationService.register(request);

        assertThat(savedUser().getRole()).extracting(Role::getRoleType).containsExactly(RoleType.CLIENT);
        verify(roleRepository, never()).findByRoleType(RoleType.SUPER_ADMIN);
    }

    @Test
    void normalRegistrationPersistsEncodedPassword() {
        authenticationService.register(registerRequest());

        User savedUser = savedUser();
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        verify(userRepository).save(any(User.class));
    }

    private RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Test");
        request.setLastName("User");
        request.setUsername("test-user");
        request.setEmail("test@example.com");
        request.setPassword("password");
        return request;
    }

    private String registrationJson(String roleType) {
        return "{" +
                "\"firstName\":\"Test\",\"lastName\":\"User\",\"username\":\"test-user\"," +
                "\"email\":\"test@example.com\",\"password\":\"password\",\"roleType\":\"" +
                roleType + "\"}";
    }

    private User savedUser() {
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        return userCaptor.getValue();
    }
}