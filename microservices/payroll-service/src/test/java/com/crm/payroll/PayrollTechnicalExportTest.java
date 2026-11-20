package com.crm.payroll;
import com.crm.payroll.entity.*;
import com.crm.payroll.repository.*;
import com.crm.payroll.service.PayrollTechnicalExport;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PayrollTechnicalExportTest {
 final PayrollRepository payrolls=mock(PayrollRepository.class);final PayrollSettlementRepository entries=mock(PayrollSettlementRepository.class);
 final PayrollTechnicalExport service=new PayrollTechnicalExport(payrolls,entries);
 Payroll row;
 @BeforeEach void setup(){row=new Payroll();row.setId(7L);row.setEmployeeId("=FORMULA()");row.setPayPeriod("2026-11");row.setNetSalary(new BigDecimal("100.10"));row.setStatus("PENDING");when(payrolls.findByPayPeriod(eq("2026-11"),any())).thenReturn(new PageImpl<>(List.of(row)));}
 @Test void headersAmountsAndFormulaProtectionAreExplicit(){String csv=new String(service.csv(PayrollSettlementPolicyTest.auth("ADMIN"),"2026-11"),StandardCharsets.UTF_8);assertTrue(csv.startsWith("\uFEFFclassification,period,"));assertTrue(csv.contains("TECHNICAL_NON_REGULATORY"));assertTrue(csv.contains("\"'=FORMULA()\""));assertTrue(csv.contains("100.10"));assertTrue(csv.contains("overtime_hourly_rate"));assertEquals(21,csv.lines().findFirst().orElseThrow().split(",").length);verify(payrolls).findByPayPeriod(eq("2026-11"),any());}
 @Test void quotingAndDangerousPrefixesAreProtected(){for(String text:List.of("=x","+x","-x","@x","\t=x"," =x"))assertTrue(PayrollTechnicalExport.cell(text).startsWith("\"'"));assertEquals("\"a,\"\"b\"",PayrollTechnicalExport.cell("a,\"b"));}
 @Test void historicPaidBalanceIsNotInvented(){row.setStatus("PAID");String csv=new String(service.csv(PayrollSettlementPolicyTest.auth("SUPER_ADMIN"),"2026-11"),StandardCharsets.UTF_8);assertTrue(csv.contains("\"0\",\"\",\"LEGACY_UNRECONCILED\""));}
 @Test void invalidPeriodAndWrongRoleNeverReadData(){assertThrows(IllegalArgumentException.class,()->service.csv(PayrollSettlementPolicyTest.auth("ADMIN"),"2026-13"));assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.csv(PayrollSettlementPolicyTest.auth("CLIENT"),"2026-11"));verifyNoInteractions(payrolls,entries);}
 @Test void oversizedPeriodIsRejectedWithoutTruncation(){when(payrolls.findByPayPeriod(anyString(),any())).thenReturn(new PageImpl<>(List.of(row),PageRequest.of(0,250),5001));assertThrows(IllegalArgumentException.class,()->service.csv(PayrollSettlementPolicyTest.auth("ADMIN"),"2026-11"));}
 @Test void cancelledOriginalKeepsAmountButNoRemainingDue(){row.setCancelled(true);String csv=new String(service.csv(PayrollSettlementPolicyTest.auth("ADMIN"),"2026-11"),StandardCharsets.UTF_8);assertTrue(csv.contains("100.10"));assertTrue(csv.contains("CANCELLED"));}
}
