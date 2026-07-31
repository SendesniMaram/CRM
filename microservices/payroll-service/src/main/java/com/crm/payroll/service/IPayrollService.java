package com.crm.payroll.service;

import com.crm.payroll.dto.PayrollRequest;
import com.crm.payroll.dto.PayrollResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IPayrollService {

    PayrollResponse createPayroll(PayrollRequest request);

    PayrollResponse getPayrollById(Long id);

    List<PayrollResponse> getAllPayrolls();

    Page<PayrollResponse> getAllPayrollsPaged(int page, int size, String sortBy, String direction, String keyword);

    PayrollResponse updatePayroll(Long id, PayrollRequest request);

    void deletePayroll(Long id);
}

