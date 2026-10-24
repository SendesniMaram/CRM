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

class LeaveAccessUidTest {
    private final LeaveEmployeeAssociationRepository links = mock(LeaveEmployeeAssociationRepository.class);
    private final LeaveAccess access = new LeaveAccess(new EmployeeAssociationService(links,
            mock(LeaveDirectoryClient.class), mock(PlatformTransactionManager.class)));

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    private void authenticate() {
        var roles = List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
        var principal = new AuthenticatedUserPrincipal(42, "renamed", roles);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, roles));
    }

    @Test void usesStableUidAndNeverLooksUpUsername() {
        authenticate();
        var link = new LeaveEmployeeAssociation();
        link.setUserId(42L); link.setUsername("old-name"); link.setEmployeeId("103");
        when(links.findByUserId(42L)).thenReturn(Optional.of(link));
        assertThat(access.ownEmployeeId()).isEqualTo("103");
        verify(links).findByUserId(42L);
        verify(links, never()).findById(anyString());
    }

    @Test void missingAssociationIsDeniedWithoutUsernameFallback() {
        authenticate();
        when(links.findByUserId(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(access::ownEmployeeId).isInstanceOf(AccessDeniedException.class);
        verify(links, never()).findById(anyString());
    }

    @Test void inconsistentAssociationIdentityIsDenied() {
        authenticate();
        var link = new LeaveEmployeeAssociation();
        link.setUserId(84L); link.setUsername("renamed"); link.setEmployeeId("103");
        when(links.findByUserId(42L)).thenReturn(Optional.of(link));
        assertThatThrownBy(access::ownEmployeeId).isInstanceOf(AccessDeniedException.class);
    }

    @Test void missingLegacyOrUnauthenticatedPrincipalIsDeniedBeforeRepositoryAccess() {
        assertThatThrownBy(access::ownEmployeeId).isInstanceOf(AccessDeniedException.class);
        var roles = List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("renamed", null, roles));
        assertThatThrownBy(access::ownEmployeeId).isInstanceOf(AccessDeniedException.class);
        var principal = new AuthenticatedUserPrincipal(42, "renamed", roles);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));
        assertThatThrownBy(access::ownEmployeeId).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(links);
    }

    @Test void administrativeAuthoritiesAndUsernameRemainCompatible() {
        for (String role : List.of("ROLE_ADMIN", "ROLE_SUPER_ADMIN")) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("validator", null,
                    List.of(new SimpleGrantedAuthority(role))));
            access.requireAdmin();
            access.checkEmployee("EMP-HISTORICAL");
            assertThat(access.authentication().getName()).isEqualTo("validator");
        }
        verifyNoInteractions(links);
    }
}
