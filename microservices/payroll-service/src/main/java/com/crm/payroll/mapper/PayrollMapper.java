package com.crm.payroll.mapper;

import com.crm.payroll.dto.PayrollRequest;
import com.crm.payroll.dto.PayrollResponse;
import com.crm.payroll.entity.Payroll;
import org.springframework.stereotype.Component;

@Component
public class PayrollMapper {

    public PayrollResponse toResponse(Payroll payroll) {
        if (payroll == null) {
            return null;
        }

        PayrollResponse response = new PayrollResponse();
        response.setId(payroll.getId());
        response.setEmployeeId(payroll.getEmployeeId());
        response.setBaseSalary(payroll.getBaseSalary());
        response.setBonuses(payroll.getBonuses());
        response.setDeductions(payroll.getDeductions());
        response.setTaxAmount(payroll.getTaxAmount());
        response.setNetSalary(payroll.getNetSalary());
        response.setPayPeriod(payroll.getPayPeriod());
        response.setPaymentDate(payroll.getPaymentDate());
        response.setCurrency(payroll.getCurrency());
        response.setStatus(payroll.getStatus());
        response.setCreatedAt(payroll.getCreatedAt());
        response.setUpdatedAt(payroll.getUpdatedAt());

        return response;
    }

    public Payroll toEntity(PayrollRequest request) {
        if (request == null) {
            return null;
        }

        Payroll payroll = new Payroll();
        payroll.setEmployeeId(request.getEmployeeId());
        payroll.setBaseSalary(request.getBaseSalary());
        payroll.setBonuses(request.getBonuses());
        payroll.setDeductions(request.getDeductions());
        payroll.setTaxAmount(request.getTaxAmount());
        payroll.setPayPeriod(request.getPayPeriod());
        payroll.setPaymentDate(request.getPaymentDate());
        payroll.setCurrency(request.getCurrency());
        payroll.setStatus(request.getStatus());

        return payroll;
    }

    public void updateEntityFromRequest(Payroll payroll, PayrollRequest request) {
        if (request == null) {
            return;
        }
        payroll.setEmployeeId(request.getEmployeeId());
        payroll.setBaseSalary(request.getBaseSalary());
        payroll.setBonuses(request.getBonuses());
        payroll.setDeductions(request.getDeductions());
        payroll.setTaxAmount(request.getTaxAmount());
        payroll.setPayPeriod(request.getPayPeriod());
        payroll.setPaymentDate(request.getPaymentDate());
        payroll.setCurrency(request.getCurrency());
        payroll.setStatus(request.getStatus());
    }
}

