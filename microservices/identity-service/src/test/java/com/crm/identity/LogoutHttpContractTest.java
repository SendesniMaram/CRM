package com.crm.identity;

import com.crm.identity.controller.AuthenticationController;
import com.crm.identity.dto.RefreshTokenRequest;
import com.crm.identity.exception.GlobalExceptionHandler;
import com.crm.identity.security.JwtService;
import com.crm.identity.service.IAuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LogoutHttpContractTest {
    @Test void logoutReturnsNoContentAndNeverEchoesItsToken() throws Exception {
        var service = mock(IAuthenticationService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AuthenticationController(service,mock(JwtService.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/api/auth/logout").contentType("application/json").content("{\"refreshToken\":\"synthetic-private\"}"))
                .andExpect(status().isNoContent()).andExpect(header().string("Cache-Control","no-store")).andExpect(content().string(""));
        verify(service).logout(argThat((RefreshTokenRequest r)->"synthetic-private".equals(r.getRefreshToken())));
    }
    @Test void missingAndOversizedTokensAreBadRequestsNotServerErrors() throws Exception {
        var service = mock(IAuthenticationService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AuthenticationController(service,mock(JwtService.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        for(String value : new String[]{"", " ", "x".repeat(501)})
            mvc.perform(post("/api/auth/logout").contentType("application/json").content("{\"refreshToken\":\""+value+"\"}"))
                    .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
