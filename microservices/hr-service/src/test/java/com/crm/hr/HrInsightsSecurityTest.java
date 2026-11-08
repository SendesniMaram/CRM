package com.crm.hr;
import com.crm.hr.config.SecurityConfig;import com.crm.hr.controller.HrInsightsController;import com.crm.hr.service.*;import com.crm.security.*;import org.junit.jupiter.api.*;import org.junit.jupiter.params.ParameterizedTest;import org.junit.jupiter.params.provider.ValueSource;import org.springframework.context.annotation.*;import org.springframework.mock.web.MockServletContext;import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;import org.springframework.web.servlet.config.annotation.EnableWebMvc;import org.springframework.test.web.servlet.MockMvc;import java.time.Instant;import java.util.List;
import static org.mockito.Mockito.*;import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/** Real filters and HTTP contracts, mocked services, no socket or database. */
class HrInsightsSecurityTest {
 AnnotationConfigWebApplicationContext context;MockMvc mvc;AccountValidationClient validation;LeaveBalanceService balances;HrStatisticsService statistics;
 @Configuration @EnableWebMvc static class Beans {
  @Bean AccountValidationClient validation(){return mock(AccountValidationClient.class);}
  @Bean JwtAuthenticationFilter jwt(AccountValidationClient c){return new JwtAuthenticationFilter(c);}
  @Bean RestAuthenticationEntryPoint entry(){return new RestAuthenticationEntryPoint();}
  @Bean CustomAccessDeniedHandler denied(){return new CustomAccessDeniedHandler();}
  @Bean LeaveBalanceService balances(){return mock(LeaveBalanceService.class);}
  @Bean HrStatisticsService statistics(){return mock(HrStatisticsService.class);}
  @Bean ITrainingService trainings(){return mock(ITrainingService.class);}
  @Bean com.crm.hr.controller.TrainingController trainingController(ITrainingService s){return new com.crm.hr.controller.TrainingController(s);}
  @Bean HrInsightsController controller(LeaveBalanceService b,HrStatisticsService s){return new HrInsightsController(b,s);}
  @Bean com.crm.hr.exception.GlobalExceptionHandler errors(){return new com.crm.hr.exception.GlobalExceptionHandler();}
 }
 @BeforeEach void setup(){context=new AnnotationConfigWebApplicationContext();context.setServletContext(new MockServletContext());context.register(Beans.class,SecurityConfig.class);context.refresh();validation=context.getBean(AccountValidationClient.class);balances=context.getBean(LeaveBalanceService.class);statistics=context.getBean(HrStatisticsService.class);mvc=webAppContextSetup(context).addFilters(context.getBean("springSecurityFilterChain",jakarta.servlet.Filter.class)).build();}
 @AfterEach void close(){context.close();org.springframework.security.core.context.SecurityContextHolder.clearContext();}
 void role(String role){when(validation.validate(any())).thenReturn(new AccountValidationResult(AccountValidationResult.Status.VALID,new JwtService.AccessTokenClaims(42,"synthetic",List.of(role),Instant.now().plusSeconds(3600))));}
 static final String PERIOD="?from=2026-01-01&to=2026-12-31";
 @ParameterizedTest @ValueSource(strings={"EMPLOYEE","CLIENT","ADMIN","SUPER_ADMIN"}) void personalBalanceRequiresEmployee(String role)throws Exception{role(role);mvc.perform(get("/api/leaves/balance/me"+PERIOD)).andExpect(status().is(role.equals("EMPLOYEE")?200:403));if(!role.equals("EMPLOYEE"))verifyNoInteractions(balances);}
 @ParameterizedTest @ValueSource(strings={"EMPLOYEE","CLIENT","ADMIN","SUPER_ADMIN"}) void administrativeBalanceRequiresAdmin(String role)throws Exception{role(role);boolean admin=role.equals("ADMIN")||role.equals("SUPER_ADMIN");mvc.perform(get("/api/leaves/balance/employees/999"+PERIOD).header("Authorization","Bearer synthetic")).andExpect(status().is(admin?200:403));if(!admin)verifyNoInteractions(balances);}
 @ParameterizedTest @ValueSource(strings={"EMPLOYEE","CLIENT","ADMIN","SUPER_ADMIN"}) void statisticsRequireAdmin(String role)throws Exception{role(role);boolean admin=role.equals("ADMIN")||role.equals("SUPER_ADMIN");mvc.perform(get("/api/hr-insights"+PERIOD)).andExpect(status().is(admin?200:403));if(!admin)verifyNoInteractions(statistics);}
 @Test void personalTargetParameterIsRejectedBeforeService()throws Exception{role("EMPLOYEE");mvc.perform(get("/api/leaves/balance/me"+PERIOD+"&employeeId=999")).andExpect(status().isBadRequest());verifyNoInteractions(balances);}
 @Test void identityUnavailableIs503()throws Exception{when(validation.validate(any())).thenReturn(AccountValidationResult.unavailable());mvc.perform(get("/api/hr-insights"+PERIOD)).andExpect(status().isServiceUnavailable());verifyNoInteractions(statistics);}
 @Test void invalidSessionIs401()throws Exception{when(validation.validate(any())).thenReturn(AccountValidationResult.invalid());mvc.perform(get("/api/leaves/balance/me"+PERIOD)).andExpect(status().isUnauthorized());verifyNoInteractions(balances);}
 @Test void personalNamespaceCannotBeWritten()throws Exception{role("EMPLOYEE");mvc.perform(post("/api/leaves/balance/me").contentType("application/json").content("{}")).andExpect(status().isForbidden());verifyNoInteractions(balances);}
 @Test void balanceAndStatisticsAreNotCacheable()throws Exception{role("EMPLOYEE");mvc.perform(get("/api/leaves/balance/me"+PERIOD)).andExpect(header().string("Cache-Control","no-store"));role("ADMIN");mvc.perform(get("/api/hr-insights"+PERIOD)).andExpect(header().string("Cache-Control","no-store"));}
 @Test void invalidDateIs400BeforeService()throws Exception{role("ADMIN");mvc.perform(get("/api/hr-insights?from=2026-02-30&to=2026-12-31")).andExpect(status().isBadRequest());verifyNoInteractions(statistics);}
 @ParameterizedTest @ValueSource(strings={"EMPLOYEE","CLIENT","ADMIN","SUPER_ADMIN"}) void trainingWriteRequiresAdminAndForwardsBearer(String role)throws Exception{
  role(role);boolean admin=role.equals("ADMIN")||role.equals("SUPER_ADMIN");
  String body="{\"employeeId\":\"101\",\"title\":\"Training\",\"provider\":\"Provider\",\"startDate\":\"2099-01-01\",\"endDate\":\"2099-01-02\",\"cost\":0,\"status\":\"PLANNED\"}";
  mvc.perform(post("/api/trainings").header("Authorization","Bearer synthetic").contentType("application/json").content(body)).andExpect(status().is(admin?201:403));
  ITrainingService training=context.getBean(ITrainingService.class);if(admin)verify(training).createTraining(any(),eq("Bearer synthetic"));else verifyNoInteractions(training);
 }
 @Test void trainingHeaderIsRequiredWhenAuthenticationIsSimulatedValid()throws Exception{role("ADMIN");String body="{\"employeeId\":\"101\",\"title\":\"Training\",\"provider\":\"Provider\",\"startDate\":\"2099-01-01\",\"endDate\":\"2099-01-02\",\"cost\":0,\"status\":\"PLANNED\"}";mvc.perform(post("/api/trainings").contentType("application/json").content(body)).andExpect(status().isBadRequest());verifyNoInteractions(context.getBean(ITrainingService.class));}
}