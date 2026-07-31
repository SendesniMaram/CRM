package com.crm.hr.service.impl;

import com.crm.hr.dto.LeaveRequestDto;
import com.crm.hr.dto.LeaveResponse;
import com.crm.hr.entity.LeaveRequest;
import com.crm.hr.exception.ResourceNotFoundException;
import com.crm.hr.mapper.LeaveMapper;
import com.crm.hr.repository.LeaveRepository;
import com.crm.hr.service.ILeaveService;
import com.crm.hr.util.LeaveSpecification;
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
public class LeaveServiceImpl implements ILeaveService {

    private final LeaveRepository leaveRepository;
    private final LeaveMapper leaveMapper;

    public LeaveServiceImpl(LeaveRepository leaveRepository, LeaveMapper leaveMapper) {
        this.leaveRepository = leaveRepository;
        this.leaveMapper = leaveMapper;
    }

    @Override
    public LeaveResponse createLeave(LeaveRequestDto request) {
        // Validate date range
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after or equal to start date");
        }

        LeaveRequest leaveRequest = leaveMapper.toEntity(request);
        LeaveRequest saved = leaveRepository.save(leaveRequest);
        return leaveMapper.toResponse(saved);
    }

    @Override
    public LeaveResponse getLeaveById(Long id) {
        LeaveRequest leaveRequest = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));
        return leaveMapper.toResponse(leaveRequest);
    }

    @Override
    public List<LeaveResponse> getAllLeaves() {
        return leaveRepository.findAll().stream()
                .map(leaveMapper::toResponse)
                .toList();
    }

    @Override
    public Page<LeaveResponse> getAllLeavesPaged(int page, int size, String sortBy, String direction, String keyword) {
        String sortField = StringUtils.hasText(sortBy) ? sortBy : "id";
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(sortDirection, sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<LeaveRequest> leavePage;

        if (StringUtils.hasText(keyword)) {
            leavePage = leaveRepository.findAll(
                    LeaveSpecification.searchByKeyword(keyword), pageable);
        } else {
            leavePage = leaveRepository.findAll(pageable);
        }

        return leavePage.map(leaveMapper::toResponse);
    }

    @Override
    public LeaveResponse updateLeave(Long id, LeaveRequestDto request) {
        LeaveRequest existing = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));

        // Prevent modification of already approved or rejected requests
        if (existing.getStatus() == LeaveRequest.LeaveStatus.APPROVED) {
            throw new IllegalArgumentException("Cannot modify an already approved leave request");
        }
        if (existing.getStatus() == LeaveRequest.LeaveStatus.REJECTED) {
            throw new IllegalArgumentException("Cannot modify an already rejected leave request");
        }

        // Validate date range
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after or equal to start date");
        }

        leaveMapper.updateEntityFromRequest(existing, request);
        LeaveRequest saved = leaveRepository.save(existing);
        return leaveMapper.toResponse(saved);
    }

    @Override
    public LeaveResponse approveLeave(Long id) {
        LeaveRequest existing = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));

        if (existing.getStatus() == LeaveRequest.LeaveStatus.APPROVED) {
            throw new IllegalArgumentException("Leave request is already approved");
        }
        if (existing.getStatus() == LeaveRequest.LeaveStatus.REJECTED) {
            throw new IllegalArgumentException("Cannot approve a rejected leave request");
        }

        existing.setStatus(LeaveRequest.LeaveStatus.APPROVED);
        LeaveRequest saved = leaveRepository.save(existing);
        return leaveMapper.toResponse(saved);
    }

    @Override
    public LeaveResponse rejectLeave(Long id) {
        LeaveRequest existing = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));

        if (existing.getStatus() == LeaveRequest.LeaveStatus.REJECTED) {
            throw new IllegalArgumentException("Leave request is already rejected");
        }
        if (existing.getStatus() == LeaveRequest.LeaveStatus.APPROVED) {
            throw new IllegalArgumentException("Cannot reject an already approved leave request");
        }

        existing.setStatus(LeaveRequest.LeaveStatus.REJECTED);
        LeaveRequest saved = leaveRepository.save(existing);
        return leaveMapper.toResponse(saved);
    }

    @Override
    public void deleteLeave(Long id) {
        LeaveRequest leaveRequest = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));
        leaveRepository.delete(leaveRequest);
    }
}
