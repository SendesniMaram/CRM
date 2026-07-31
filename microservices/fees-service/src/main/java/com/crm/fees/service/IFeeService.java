package com.crm.fees.service;

import com.crm.fees.dto.FeeRequest;
import com.crm.fees.dto.FeeResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IFeeService {

    FeeResponse createFee(FeeRequest request);

    FeeResponse getFeeById(Long id);

    List<FeeResponse> getAllFees();

    Page<FeeResponse> getAllFeesPaged(int page, int size, String sortBy, String direction, String keyword);

    FeeResponse updateFee(Long id, FeeRequest request);

    void deleteFee(Long id);
}
