package com.crm.hr.service;

import com.crm.hr.client.LeaveDirectoryClient;
import com.crm.hr.entity.LeaveEmployeeAssociation;
import com.crm.hr.repository.LeaveEmployeeAssociationRepository;
import com.crm.security.AuthenticatedUserPrincipal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeAssociationServiceTest {
    private final LeaveEmployeeAssociationRepository links = mock(LeaveEmployeeAssociationRepository.class);
    private final EmployeeAssociationService service = new EmployeeAssociationService(links,
            mock(LeaveDirectoryClient.class), mock(PlatformTransactionManager.class));

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void absentAndLegacyPrincipalsCannotResolveAnAssociation() {
        assertThatThrownBy(() -> service.readOwn(null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.readOwn(new UsernamePasswordAuthenticationToken("alice", null,
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")))))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(links);
    }

    @Test void typedButUnauthenticatedPrincipalCannotResolveAnAssociation() {
        var principal = new AuthenticatedUserPrincipal(42, "alice", List.of());
        assertThatThrownBy(() -> service.readOwn(new UsernamePasswordAuthenticationToken(principal, null)))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(links);
    }

    @Test void typedAdministratorDoesNotImplicitlyBecomeAnEmployee() {
        var roles = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        var principal = new AuthenticatedUserPrincipal(42, "alice", roles);
        assertThatThrownBy(() -> service.readOwn(new UsernamePasswordAuthenticationToken(principal, null, roles)))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(links);
    }

    @Test void typedIdentityResolvesByUidWithoutUsernameLookup() {
        var roles = List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
        var principal = new AuthenticatedUserPrincipal(42, "renamed", roles);
        var link = new LeaveEmployeeAssociation();
        link.setUserId(42L); link.setUsername("old-name"); link.setEmployeeId("103");
        when(links.findByUserId(42L)).thenReturn(Optional.of(link));
        assertThat(service.readOwn(new UsernamePasswordAuthenticationToken(principal, null, roles))).isSameAs(link);
        verify(links).findByUserId(42L);
        verify(links, never()).findById(anyString());
    }
}
