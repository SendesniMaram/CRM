package com.crm.hr.service.impl;

import com.crm.hr.dto.HrRequest;
import com.crm.hr.dto.HrResponse;
import com.crm.hr.entity.HrRecord;
import com.crm.hr.exception.ResourceNotFoundException;
import com.crm.hr.mapper.HrMapper;
import com.crm.hr.repository.HrRepository;
import com.crm.hr.service.IHrService;
import com.crm.hr.util.HrSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
public class HrServiceImpl implements IHrService {

    private final HrRepository hrRepository;
    private final HrMapper hrMapper;

    public HrServiceImpl(HrRepository hrRepository,
                         HrMapper hrMapper) {
        this.hrRepository = hrRepository;
        this.hrMapper = hrMapper;
    }

    @Override
    public HrResponse createRecord(HrRequest request) {
        if (hrRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new IllegalArgumentException("Employee ID already exists: " + request.getEmployeeId());
        }

        HrRecord record = hrMapper.toEntity(request);
        HrRecord saved = hrRepository.save(record);
        return hrMapper.toResponse(saved);
    }

    @Override
    public HrResponse getRecordById(Long id) {
        HrRecord record = hrRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HR record not found with id: " + id));
        return hrMapper.toResponse(record);
    }

    @Override
    public List<HrResponse> getAllRecords() {
        return hrRepository.findAll().stream()
                .map(hrMapper::toResponse)
                .toList();
    }

    @Override
    public Page<HrResponse> getAllRecordsPaged(int page, int size, String sortBy, String direction, String keyword) {
        String sortField = StringUtils.hasText(sortBy) ? sortBy : "id";
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(sortDirection, sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<HrRecord> recordPage;

        if (StringUtils.hasText(keyword)) {
            recordPage = hrRepository.findAll(
                    HrSpecification.searchByKeyword(keyword), pageable);
        } else {
            recordPage = hrRepository.findAll(pageable);
        }

        return recordPage.map(hrMapper::toResponse);
    }

    @Override
    public HrResponse updateRecord(Long id, HrRequest request) {
        HrRecord existing = hrRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HR record not found with id: " + id));

        if (!existing.getEmployeeId().equals(request.getEmployeeId())
                && hrRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new IllegalArgumentException("Employee ID already exists: " + request.getEmployeeId());
        }

        hrMapper.updateEntityFromRequest(existing, request);
        HrRecord saved = hrRepository.save(existing);
        return hrMapper.toResponse(saved);
    }

    @Override
    public void deleteRecord(Long id) {
        HrRecord record = hrRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HR record not found with id: " + id));
        hrRepository.delete(record);
    }
}
