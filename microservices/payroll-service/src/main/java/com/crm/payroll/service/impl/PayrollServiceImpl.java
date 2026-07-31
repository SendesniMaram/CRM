package com.crm.payroll.service.impl;

import com.crm.payroll.dto.PayrollRequest;
import com.crm.payroll.dto.PayrollResponse;
import com.crm.payroll.entity.Payroll;
import com.crm.payroll.exception.ResourceNotFoundException;
import com.crm.payroll.mapper.PayrollMapper;
import com.crm.payroll.repository.PayrollRepository;
import com.crm.payroll.service.IPayrollService;
import com.crm.payroll.util.PayrollSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class PayrollServiceImpl implements IPayrollService {

    private final PayrollRepository payrollRepository;
    private final PayrollMapper payrollMapper;

    public PayrollServiceImpl(PayrollRepository payrollRepository,
                              PayrollMapper payrollMapper) {
        this.payrollRepository = payrollRepository;
        this.payrollMapper = payrollMapper;
    }

    @Override
    public PayrollResponse createPayroll(PayrollRequest request) {
        Payroll payroll = payrollMapper.toEntity(request);

        // Calculate net salary automatically: netSalary = baseSalary + bonuses - deductions - taxAmount
        BigDecimal netSalary = calculateNetSalary(request.getBaseSalary(), request.getBonuses(), request.getDeductions(), request.getTaxAmount());
        payroll.setNetSalary(netSalary);

        Payroll saved = payrollRepository.save(payroll);
        return payrollMapper.toResponse(saved);
    }

    @Override
    public PayrollResponse getPayrollById(Long id) {
        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found with id: " + id));
        return payrollMapper.toResponse(payroll);
    }

    @Override
    public List<PayrollResponse> getAllPayrolls() {
        return payrollRepository.findAll().stream()
                .map(payrollMapper::toResponse)
                .toList();
    }

    @Override
    public Page<PayrollResponse> getAllPayrollsPaged(int page, int size, String sortBy, String direction, String keyword) {
        String sortField = StringUtils.hasText(sortBy) ? sortBy : "id";
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(sortDirection, sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Payroll> payrollPage;

        if (StringUtils.hasText(keyword)) {
            payrollPage = payrollRepository.findAll(
                    PayrollSpecification.searchByKeyword(keyword), pageable);
        } else {
            payrollPage = payrollRepository.findAll(pageable);
        }

        return payrollPage.map(payrollMapper::toResponse);
    }

    @Override
    public PayrollResponse updatePayroll(Long id, PayrollRequest request) {
        Payroll existing = payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found with id: " + id));

        payrollMapper.updateEntityFromRequest(existing, request);

        // Recalculate net salary on update
        BigDecimal netSalary = calculateNetSalary(request.getBaseSalary(), request.getBonuses(), request.getDeductions(), request.getTaxAmount());
        existing.setNetSalary(netSalary);

        Payroll saved = payrollRepository.save(existing);
        return payrollMapper.toResponse(saved);
    }

    @Override
    public void deletePayroll(Long id) {
        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found with id: " + id));
        payrollRepository.delete(payroll);
    }

    /**
     * Calculate net salary: netSalary = baseSalary + bonuses - deductions - taxAmount
     */
    private BigDecimal calculateNetSalary(BigDecimal baseSalary, BigDecimal bonuses, BigDecimal deductions, BigDecimal taxAmount) {
        BigDecimal base = baseSalary != null ? baseSalary : BigDecimal.ZERO;
        BigDecimal bonus = bonuses != null ? bonuses : BigDecimal.ZERO;
        BigDecimal deduction = deductions != null ? deductions : BigDecimal.ZERO;
        BigDecimal tax = taxAmount != null ? taxAmount : BigDecimal.ZERO;

        return base.add(bonus).subtract(deduction).subtract(tax);
    }
}

