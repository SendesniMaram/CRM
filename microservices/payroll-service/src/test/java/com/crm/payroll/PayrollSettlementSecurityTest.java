package com.crm.payroll;
import com.crm.payroll.config.SecurityConfig;
import com.crm.payroll.controller.PayrollSettlementController;
import com.crm.payroll.service.*;
import com.crm.payroll.service.impl.PayrollServiceImpl;
import com.crm.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/** Real security filters, mocked application services, no network or database. */
class PayrollSettlementSecurityTest {
 AnnotationConfigWebApplicationContext context;MockMvc mvc;AccountValidationClient validation;PayrollSettlementService service;
 @Configuration @EnableWebMvc static class Beans {
  @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
  @Bean AccountValidationClient validation(){return mock(AccountValidationClient.class);}
  @Bean JwtAuthenticationFilter jwt(AccountValidationClient client){return new JwtAuthenticationFilter(client);}
  @Bean RestAuthenticationEntryPoint entry(){return new RestAuthenticationEntryPoint();}
  @Bean CustomAccessDeniedHandler denied(){return new CustomAccessDeniedHandler();}
  @Bean PayrollSettlementService settlements(){return mock(PayrollSettlementService.class);}
  @Bean PayrollServiceImpl payroll(){return mock(PayrollServiceImpl.class);}
  @Bean PayrollTechnicalExport export(){return mock(PayrollTechnicalExport.class);}
  @Bean PayrollSettlementController controller(PayrollSettlementService s,PayrollServiceImpl p,PayrollTechnicalExport e){return new PayrollSettlementController(s,p,e);}
 }
 @BeforeEach void setup(){context=new AnnotationConfigWebApplicationContext();context.setServletContext(new MockServletContext());context.register(Beans.class,SecurityConfig.class);context.refresh();validation=context.getBean(AccountValidationClient.class);service=context.getBean(PayrollSettlementService.class);mvc=webAppContextSetup(context).addFilters(context.getBean("springSecurityFilterChain",jakarta.servlet.Filter.class)).build();}
 @AfterEach void close(){context.close();org.springframework.security.core.context.SecurityContextHolder.clearContext();}
 void role(String role){when(validation.validate(any())).thenReturn(new AccountValidationResult(AccountValidationResult.Status.VALID,new JwtService.AccessTokenClaims(42,"operator",List.of(role),Instant.now().plusSeconds(3600))));}
 @ParameterizedTest @ValueSource(strings={"EMPLOYEE","CLIENT","ADMIN","SUPER_ADMIN"}) void adminReadAndExportRequireAdmin(String role)throws Exception{role(role);boolean admin=role.equals("ADMIN")||role.equals("SUPER_ADMIN");mvc.perform(get("/api/payrolls/7/settlements")).andExpect(status().is(admin?200:403));mvc.perform(get("/api/payrolls/export/technical.csv?period=2026-11")).andExpect(status().is(admin?200:403));if(!admin)verifyNoInteractions(service,context.getBean(PayrollTechnicalExport.class));}
 @ParameterizedTest @ValueSource(strings={"EMPLOYEE","CLIENT","ADMIN","SUPER_ADMIN"}) void personalRequiresEmployee(String role)throws Exception{role(role);mvc.perform(get("/api/payrolls/me/7/settlements").header("Authorization","Bearer synthetic")).andExpect(status().is(role.equals("EMPLOYEE")?200:403));if(!role.equals("EMPLOYEE"))verifyNoInteractions(service);}
 @ParameterizedTest @ValueSource(strings={"EMPLOYEE","CLIENT"}) void nonAdminsCannotWrite(String role)throws Exception{role(role);for(String path:List.of("/api/payrolls/7/settlements","/api/payrolls/7/corrections","/api/payrolls/7/settlements/ref/reversal"))mvc.perform(post(path).contentType("application/json").content("{}")).andExpect(status().isForbidden());verifyNoInteractions(service,context.getBean(PayrollServiceImpl.class));}
 @Test void invalidSessionIs401()throws Exception{when(validation.validate(any())).thenReturn(AccountValidationResult.invalid());mvc.perform(get("/api/payrolls/7/settlements")).andExpect(status().isUnauthorized());verifyNoInteractions(service);}
 @Test void unavailableIdentityIs503()throws Exception{when(validation.validate(any())).thenReturn(AccountValidationResult.unavailable());mvc.perform(get("/api/payrolls/7/settlements")).andExpect(status().isServiceUnavailable());verifyNoInteractions(service);}
 @Test void invalidPaymentIs400BeforeService()throws Exception{role("ADMIN");mvc.perform(post("/api/payrolls/7/settlements").contentType("application/json").content("{\"requestId\":\"synthetic\",\"amount\":0,\"paidAt\":\"2026-11-02\"}")).andExpect(status().isBadRequest());verifyNoInteractions(service);}
 @Test void noStoreForSuccessfulReads()throws Exception{role("ADMIN");mvc.perform(get("/api/payrolls/7/settlements")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));}
}
