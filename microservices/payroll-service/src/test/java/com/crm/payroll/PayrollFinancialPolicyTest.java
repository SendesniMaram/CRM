package com.crm.payroll;

import com.crm.payroll.dto.PayrollRequest;
import com.crm.payroll.mapper.PayrollMapper;
import com.crm.payroll.repository.PayrollRepository;
import com.crm.payroll.service.impl.PayrollServiceImpl;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayrollFinancialPolicyTest {
    PayrollRepository repository;
    PayrollServiceImpl service;
    PayrollRequest request;
    @BeforeEach void setup() {
        repository = mock(PayrollRepository.class); service = new PayrollServiceImpl(repository, new PayrollMapper(), mock(com.crm.payroll.repository.PayrollSettlementRepository.class));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        request = new PayrollRequest(); request.setEmployeeId("legacy-id"); request.setPayPeriod("2026-10");
        request.setStatus("PENDING"); request.setBaseSalary(new BigDecimal("100.00"));
    }
    @Test void optionalAmountsAreZeroAndNetIsStillCalculated() {
        assertEquals(0, service.createPayroll(request).getNetSalary().compareTo(new BigDecimal("100.00")));
        request.setBonuses(new BigDecimal("10")); request.setDeductions(new BigDecimal("20")); request.setTaxAmount(new BigDecimal("5"));
        assertEquals(0, service.createPayroll(request).getNetSalary().compareTo(new BigDecimal("85")));
    }
    @ParameterizedTest @ValueSource(strings = {"-0.01", "0.001", "10000000000"})
    void rejectsInvalidAdjustments(String amount) {
        request.setBonuses(new BigDecimal(amount)); assertThrows(IllegalArgumentException.class, () -> service.createPayroll(request));
        verify(repository, never()).save(any());
    }
    @ParameterizedTest @ValueSource(strings = {"2026-00", "2026-13", "2026", "2026-1", "free"})
    void rejectsInvalidPeriods(String period) {
        request.setPayPeriod(period); assertThrows(IllegalArgumentException.class, () -> service.createPayroll(request));
    }
    @Test void rejectsUnknownStatusMissingFieldsNegativeNetAndCalculatedOverflow() {
        request.setStatus("unknown"); assertThrows(IllegalArgumentException.class, () -> service.createPayroll(request));
        request.setStatus("PAID"); request.setEmployeeId(" "); assertThrows(IllegalArgumentException.class, () -> service.createPayroll(request));
        request.setStatus("PENDING"); request.setEmployeeId("7"); request.setDeductions(new BigDecimal("101")); assertThrows(IllegalArgumentException.class, () -> service.createPayroll(request));
        request.setDeductions(null); request.setBaseSalary(new BigDecimal("9999999999.99")); request.setBonuses(BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class, () -> service.createPayroll(request));
    }
    @Test void updateIsValidatedBeforeMutation() {
        request.setTaxAmount(new BigDecimal("-1")); assertThrows(IllegalArgumentException.class, () -> service.updatePayroll(1L, request));
        verifyNoInteractions(repository);
    }
}
