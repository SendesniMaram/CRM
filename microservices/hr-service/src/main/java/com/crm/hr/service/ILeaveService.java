package com.crm.hr.service;

import com.crm.hr.dto.LeaveRequestDto;
import com.crm.hr.dto.LeaveResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ILeaveService {

    LeaveResponse createLeave(LeaveRequestDto request);

    LeaveResponse getLeaveById(Long id);

    List<LeaveResponse> getAllLeaves();

    Page<LeaveResponse> getAllLeavesPaged(int page, int size, String sortBy, String direction, String keyword);

    LeaveResponse updateLeave(Long id, LeaveRequestDto request);

    LeaveResponse approveLeave(Long id);

    LeaveResponse rejectLeave(Long id);

    void deleteLeave(Long id);
}
