package com.crm.hr.service;

import com.crm.hr.dto.HrRequest;
import com.crm.hr.dto.HrResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IHrService {

    HrResponse createRecord(HrRequest request);

    HrResponse getRecordById(Long id);

    List<HrResponse> getAllRecords();

    Page<HrResponse> getAllRecordsPaged(int page, int size, String sortBy, String direction, String keyword);

    HrResponse updateRecord(Long id, HrRequest request);

    void deleteRecord(Long id);
}
