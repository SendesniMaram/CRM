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

    public FeeServiceImpl(FeeRepository feeRepository,
                          FeeMapper feeMapper) {
        this.feeRepository = feeRepository;
        this.feeMapper = feeMapper;
    }

    @Override
    public FeeResponse createFee(FeeRequest request) {
        Fee fee = feeMapper.toEntity(request);

        // Calculate amount automatically: amount = hourlyRate * workedHours
        BigDecimal amount = calculateAmount(request.getHourlyRate(), request.getWorkedHours());
        fee.setAmount(amount);

        Fee saved = feeRepository.save(fee);
        return feeMapper.toResponse(saved);
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

    @Override
    public FeeResponse updateFee(Long id, FeeRequest request) {
        Fee existing = feeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fee not found with id: " + id));

        feeMapper.updateEntityFromRequest(existing, request);

        // Recalculate amount on update
        BigDecimal amount = calculateAmount(request.getHourlyRate(), request.getWorkedHours());
        existing.setAmount(amount);

        Fee saved = feeRepository.save(existing);
        return feeMapper.toResponse(saved);
    }

    @Override
    public void deleteFee(Long id) {
        Fee fee = feeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fee not found with id: " + id));
        feeRepository.delete(fee);
    }

    /**
     * Calculate amount: amount = hourlyRate * workedHours
     */
    private BigDecimal calculateAmount(BigDecimal hourlyRate, BigDecimal workedHours) {
        BigDecimal rate = hourlyRate != null ? hourlyRate : BigDecimal.ZERO;
        BigDecimal hours = workedHours != null ? workedHours : BigDecimal.ZERO;
        return rate.multiply(hours);
    }
}
