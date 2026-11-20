package com.crm.payroll;
import com.crm.payroll.dto.PayrollRequest;
import com.crm.payroll.service.PayrollCalculation;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
class PayrollVariableCalculationTest {
 PayrollRequest request;
 @BeforeEach void setup(){request=new PayrollRequest();request.setEmployeeId("103");request.setPayPeriod("2026-11");request.setStatus("PENDING");request.setBaseSalary(new BigDecimal("100"));}
 @Test void absentOvertimePreservesOldFormula(){request.setBonuses(new BigDecimal("10"));request.setDeductions(new BigDecimal("20"));request.setTaxAmount(new BigDecimal("5"));assertEquals(0,new BigDecimal("85").compareTo(PayrollCalculation.net(request)));}
 @Test void explicitRateAndHoursAreAdded(){request.setOvertimeHours(new BigDecimal("2.50"));request.setOvertimeHourlyRate(new BigDecimal("12.30"));assertEquals(new BigDecimal("30.75"),PayrollCalculation.overtime(request));assertEquals(new BigDecimal("130.75"),PayrollCalculation.net(request));}
 @Test void exactHalfCentRoundsUp(){request.setOvertimeHours(new BigDecimal("0.50"));request.setOvertimeHourlyRate(new BigDecimal("0.01"));assertEquals(new BigDecimal("0.01"),PayrollCalculation.overtime(request));}
 @Test void lessThanHalfCentRoundsDown(){request.setOvertimeHours(new BigDecimal("0.49"));request.setOvertimeHourlyRate(new BigDecimal("0.01"));assertEquals(new BigDecimal("0.00"),PayrollCalculation.overtime(request));}
 @Test void positiveHoursNeedExplicitPositiveRate(){request.setOvertimeHours(BigDecimal.ONE);assertThrows(IllegalArgumentException.class,()->PayrollCalculation.net(request));request.setOvertimeHourlyRate(BigDecimal.ZERO);assertThrows(IllegalArgumentException.class,()->PayrollCalculation.net(request));}
 @Test void rateAloneIsInvalid(){request.setOvertimeHourlyRate(BigDecimal.ONE);assertThrows(IllegalArgumentException.class,()->PayrollCalculation.net(request));}
 @Test void zeroPairIsExplicitlyAccepted(){request.setOvertimeHours(BigDecimal.ZERO);request.setOvertimeHourlyRate(BigDecimal.ZERO);assertEquals(0,request.getBaseSalary().compareTo(PayrollCalculation.net(request)));}
 @Test void invalidPrecisionAndNegativesAreRejectedAtServiceBoundary(){for(String n:new String[]{"-1","0.001","1000000"}){request.setOvertimeHours(new BigDecimal(n));request.setOvertimeHourlyRate(BigDecimal.ONE);assertThrows(IllegalArgumentException.class,()->PayrollCalculation.net(request));}}
 @Test void overtimeProductAndFinalNetAreBounded(){request.setOvertimeHours(new BigDecimal("999999.99"));request.setOvertimeHourlyRate(new BigDecimal("9999999999.99"));assertThrows(IllegalArgumentException.class,()->PayrollCalculation.net(request));request.setOvertimeHours(BigDecimal.ONE);request.setOvertimeHourlyRate(BigDecimal.ONE);request.setBaseSalary(new BigDecimal("9999999999.99"));assertThrows(IllegalArgumentException.class,()->PayrollCalculation.net(request));}
 @Test void netCannotBeNegativeAndDecimalCancellationIsExact(){request.setBaseSalary(new BigDecimal("0.30"));request.setDeductions(new BigDecimal("0.10"));request.setTaxAmount(new BigDecimal("0.20"));assertEquals(0,PayrollCalculation.net(request).signum());request.setTaxAmount(new BigDecimal("0.21"));assertThrows(IllegalArgumentException.class,()->PayrollCalculation.net(request));}
}
