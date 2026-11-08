package com.crm.fees.service.impl;

import com.crm.fees.dto.FeeRequest;
import com.crm.fees.dto.FeeResponse;
import com.crm.fees.entity.Fee;
import com.crm.fees.exception.ResourceNotFoundException;
import com.crm.fees.mapper.FeeMapper;
import com.crm.fees.repository.FeeRepository;
import com.crm.fees.service.IFeeService;
import com.crm.fees.util.FeeSpecification;
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
public class FeeServiceImpl implements IFeeService {

    private final FeeRepository feeRepository;
    private final FeeMapper feeMapper;
    private final com.crm.fees.service.FeeReferenceDirectory directory;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;

    public FeeServiceImpl(FeeRepository feeRepository,
                          FeeMapper feeMapper, com.crm.fees.service.FeeReferenceDirectory directory) {
        this.feeRepository = feeRepository;
        this.feeMapper = feeMapper;
        this.directory=directory;
    }

    @Override public FeeResponse createFee(FeeRequest request,String authorization){
        com.crm.fees.service.FeeFinance.validate(request);if(request.getWorkDate()!=null)com.crm.fees.service.FeeCycleService.date(request.getWorkDate());BigDecimal amount=com.crm.fees.service.FeeFinance.amount(request.getHourlyRate(),request.getWorkedHours());directory.validate(request.getEmployeeId(),request.getCustomerId(),authorization);Fee fee=feeMapper.toEntity(request);fee.setAmount(amount);return feeMapper.toResponse(feeRepository.save(fee));
    }
    @Override
    public FeeResponse getFeeById(Long id) {
        Fee fee = feeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fee not found with id: " + id));
        return feeMapper.toResponse(fee);
    }

    @Override
    public List<FeeResponse> getAllFees() {
        return feeRepository.findAll().stream()
                .map(feeMapper::toResponse)
                .toList();
    }

    @Override
    public Page<FeeResponse> getAllFeesPaged(int page, int size, String sortBy, String direction, String keyword) {
        String sortField = StringUtils.hasText(sortBy) ? sortBy : "id";
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(sortDirection, sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Fee> feePage;

        if (StringUtils.hasText(keyword)) {
            feePage = feeRepository.findAll(
                    FeeSpecification.searchByKeyword(keyword), pageable);
        } else {
            feePage = feeRepository.findAll(pageable);
        }

        return feePage.map(feeMapper::toResponse);
    }

    @Override public FeeResponse updateFee(Long id,FeeRequest request,String authorization){
        com.crm.fees.service.FeeFinance.validate(request);if(request.getWorkDate()!=null)com.crm.fees.service.FeeCycleService.date(request.getWorkDate());BigDecimal amount=com.crm.fees.service.FeeFinance.amount(request.getHourlyRate(),request.getWorkedHours());Fee existing=locked(id);editable(existing);
        if(!java.util.Objects.equals(existing.getEmployeeId(),request.getEmployeeId())||!java.util.Objects.equals(existing.getCustomerId(),request.getCustomerId()))directory.validate(request.getEmployeeId(),request.getCustomerId(),authorization);
        feeMapper.updateEntityFromRequest(existing,request);existing.setAmount(amount);return feeMapper.toResponse(feeRepository.save(existing));
    }
    @Override public void deleteFee(Long id){Fee row=locked(id);editable(row);feeRepository.delete(row);}
    private Fee locked(Long id){Fee row=feeRepository.lockById(id).orElseThrow(()->new ResourceNotFoundException("Fee unavailable"));if(em!=null)em.refresh(row,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);return row;}
    private void editable(Fee row){if(row.getSourceTimeId()!=null||!open(row.getInvoiceStatus())||!open(row.getPaymentStatus()))throw com.crm.fees.service.FeeFinance.conflict();}
    private boolean open(String status){return status==null||status.isBlank()||java.util.Set.of("PENDING","UNPAID","UNBILLED","NOT_INVOICED","À FACTURER").contains(status.trim().toUpperCase(java.util.Locale.ROOT));}
}