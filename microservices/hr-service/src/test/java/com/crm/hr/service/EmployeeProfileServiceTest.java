package com.crm.hr.service;

import com.crm.hr.client.EmployeeProfileClient;
import com.crm.hr.entity.LeaveEmployeeAssociation;
import com.crm.hr.exception.EmployeeProfileException;
import com.crm.security.AuthenticatedUserPrincipal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeProfileServiceTest {
    @Test void incompatiblePrincipalNeverResolvesAssociation() {
        var associations = mock(EmployeeAssociationService.class); var client = mock(EmployeeProfileClient.class);
        var service = new EmployeeProfileService(associations, client);
        assertThatThrownBy(() -> service.own(new UsernamePasswordAuthenticationToken("legacy-name", null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")))))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verifyNoInteractions(associations, client);
    }
    @Test void mismatchedAssociationUidNeverCallsEmployee() {
        var associations = mock(EmployeeAssociationService.class); var client = mock(EmployeeProfileClient.class);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
        var authentication = new UsernamePasswordAuthenticationToken(new AuthenticatedUserPrincipal(42, "employee", authorities), null, authorities);
        var link = new LeaveEmployeeAssociation(); link.setUserId(43L); link.setEmployeeId("103");
        when(associations.readOwn(authentication)).thenReturn(link);
        assertThatThrownBy(() -> new EmployeeProfileService(associations, client).own(authentication))
                .isInstanceOfSatisfying(EmployeeProfileException.class, ex -> assertThat(ex.status()).isEqualTo(502));
        verifyNoInteractions(client);
    }
}
