package com.crm.hr;
import com.crm.hr.config.SecurityConfig;
import com.crm.hr.controller.PersonalDocumentController;
import com.crm.hr.service.PersonalDocumentService;
import com.crm.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real HR security chain in a mock servlet context, without Boot or any listener/socket. */
class PersonalDocumentSecurityTest {
 AnnotationConfigWebApplicationContext context;MockMvc mvc;AccountValidationClient validation;PersonalDocumentService service;
 @Configuration @EnableWebMvc static class Beans {
  @Bean AccountValidationClient validation(){return mock(AccountValidationClient.class);}
  @Bean JwtAuthenticationFilter jwt(AccountValidationClient client){return new JwtAuthenticationFilter(client);}
  @Bean RestAuthenticationEntryPoint entry(){return new RestAuthenticationEntryPoint();}
  @Bean CustomAccessDeniedHandler denied(){return new CustomAccessDeniedHandler();}
  @Bean PersonalDocumentService service(){return mock(PersonalDocumentService.class);}
  @Bean PersonalDocumentController controller(PersonalDocumentService service){return new PersonalDocumentController(service);}
 }
 @BeforeEach void setup(){context=new AnnotationConfigWebApplicationContext();context.setServletContext(new MockServletContext());context.register(Beans.class,SecurityConfig.class);context.refresh();validation=context.getBean(AccountValidationClient.class);service=context.getBean(PersonalDocumentService.class);mvc=webAppContextSetup(context).addFilters(context.getBean("springSecurityFilterChain",jakarta.servlet.Filter.class)).build();}
 @AfterEach void close(){context.close();org.springframework.security.core.context.SecurityContextHolder.clearContext();}
 void role(String role){when(validation.validate(any())).thenReturn(new AccountValidationResult(AccountValidationResult.Status.VALID,new JwtService.AccessTokenClaims(42,"employee",List.of(role),Instant.now().plusSeconds(3600))));}
 @ParameterizedTest @ValueSource(strings={"EMPLOYEE","CLIENT","ADMIN","SUPER_ADMIN"})
 void personalRouteRequiresEmployee(String role)throws Exception{role(role);mvc.perform(get("/api/documents/me")).andExpect(status().is(role.equals("EMPLOYEE")?200:403));if(!role.equals("EMPLOYEE"))verifyNoInteractions(service);}
 @Test void invalidSessionCannotReadDocuments()throws Exception{when(validation.validate(any())).thenReturn(AccountValidationResult.invalid());mvc.perform(get("/api/documents/me")).andExpect(status().isUnauthorized());verifyNoInteractions(service);}
 @Test void identityUnavailableCannotReadDocuments()throws Exception{when(validation.validate(any())).thenReturn(AccountValidationResult.unavailable());mvc.perform(get("/api/documents/me")).andExpect(status().isServiceUnavailable());verifyNoInteractions(service);}
 @Test void employeeCannotUseAdministrativeDocumentPath()throws Exception{role("EMPLOYEE");mvc.perform(get("/api/documents/7/content")).andExpect(status().isForbidden());verifyNoInteractions(service);}
 @Test void personalNamespaceDoesNotAllowWrites()throws Exception{role("EMPLOYEE");mvc.perform(delete("/api/documents/me/7")).andExpect(status().isForbidden());verifyNoInteractions(service);}
}
