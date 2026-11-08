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
    private final com.crm.payroll.repository.PayrollSettlementRepository settlements;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

    public PayrollServiceImpl(PayrollRepository payrollRepository,
                              PayrollMapper payrollMapper, com.crm.payroll.repository.PayrollSettlementRepository settlements) {
        this.payrollRepository = payrollRepository;
        this.payrollMapper = payrollMapper;
        this.settlements = settlements;
    }

    @Override
    public PayrollResponse createPayroll(PayrollRequest request) {
        com.crm.payroll.service.FinancialValidation.request(request);
        if(!"PENDING".equals(request.getStatus()))throw conflict();
        Payroll payroll = payrollMapper.toEntity(request);

        // Calculate net salary automatically: netSalary = baseSalary + bonuses - deductions - taxAmount
        BigDecimal netSalary = com.crm.payroll.service.PayrollCalculation.net(request);
        payroll.setNetSalary(netSalary);
        payroll.setOvertimeAmount(request.getOvertimeHours()==null?null:com.crm.payroll.service.PayrollCalculation.overtime(request));

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
        com.crm.payroll.service.FinancialValidation.request(request);
        Payroll existing = locked(id);
        editable(existing);
        if(existing.getCorrectionOfId()!=null&&(!java.util.Objects.equals(existing.getEmployeeId(),request.getEmployeeId())||!java.util.Objects.equals(existing.getCurrency(),request.getCurrency())))throw conflict();
        if(!"PENDING".equals(request.getStatus()))throw conflict();

        BigDecimal netSalary = com.crm.payroll.service.PayrollCalculation.net(request);
        payrollMapper.updateEntityFromRequest(existing, request);
        existing.setNetSalary(netSalary);
        existing.setOvertimeAmount(request.getOvertimeHours()==null?null:com.crm.payroll.service.PayrollCalculation.overtime(request));

        Payroll saved = payrollRepository.save(existing);
        return payrollMapper.toResponse(saved);
    }

    @Override
    public void deletePayroll(Long id) {
        Payroll payroll = locked(id);
        editable(payroll);
        if(payroll.getCorrectionOfId()!=null||payroll.getReplacementPayrollId()!=null)throw conflict();
        payrollRepository.delete(payroll);
    }

    @Override
    public PayrollResponse setPublished(Long id, boolean published) {
        Payroll payroll = locked(id);
        if(payroll.isCancelled()||payroll.isPublished()&&!published)throw conflict();
        payroll.setPublished(published);
        return payrollMapper.toResponse(payrollRepository.save(payroll));
    }

    private Payroll locked(Long id){
        Payroll row=payrollRepository.lockById(id).orElseThrow(()->new ResourceNotFoundException("Payroll unavailable"));
        if(entityManager!=null)entityManager.refresh(row,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);return row;
    }
    private void editable(Payroll row){if(row.isPublished()||row.isCancelled()||!"PENDING".equals(row.getStatus())||settlements.existsByPayrollId(row.getId()))throw conflict();}
    private org.springframework.web.server.ResponseStatusException conflict(){return new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Published, paid or historical payroll cannot be changed directly");}
    public PayrollResponse correct(org.springframework.security.core.Authentication auth,Long id,String reason){
        long actor=com.crm.payroll.service.PayrollAuthorization.admin(auth);
        if(reason==null||reason.isBlank()||reason.length()>500)throw new IllegalArgumentException("Correction reason required");
        Payroll original=locked(id);
        var history=settlements.findByPayrollIdOrderByCreatedAtAscIdAsc(id);
        if(!original.isPublished()||original.isCancelled()||com.crm.payroll.service.PayrollLedger.legacy(original,history)||com.crm.payroll.service.PayrollLedger.paid(history).signum()!=0)throw conflict();
        PayrollRequest r=new PayrollRequest();r.setEmployeeId(original.getEmployeeId());r.setBaseSalary(original.getBaseSalary());r.setBonuses(original.getBonuses());r.setDeductions(original.getDeductions());r.setTaxAmount(original.getTaxAmount());r.setOvertimeHours(original.getOvertimeHours());r.setOvertimeHourlyRate(original.getOvertimeHourlyRate());r.setPayPeriod(original.getPayPeriod());r.setCurrency(original.getCurrency());r.setStatus("PENDING");
        com.crm.payroll.service.PayrollCalculation.net(r);
        Payroll replacement=payrollMapper.toEntity(r);replacement.setNetSalary(com.crm.payroll.service.PayrollCalculation.net(r));replacement.setOvertimeAmount(r.getOvertimeHours()==null?null:com.crm.payroll.service.PayrollCalculation.overtime(r));replacement.setCorrectionOfId(id);
        replacement=payrollRepository.saveAndFlush(replacement);
        original.setCancelled(true);original.setReplacementPayrollId(replacement.getId());original.setCorrectionReason(reason.trim());original.setCorrectedBy(actor);original.setCorrectedAt(java.time.LocalDateTime.now());
        return payrollMapper.toResponse(replacement);
    }
}
