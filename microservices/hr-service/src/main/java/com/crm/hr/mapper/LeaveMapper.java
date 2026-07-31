package com.crm.hr.mapper;

import com.crm.hr.dto.LeaveRequestDto;
import com.crm.hr.dto.LeaveResponse;
import com.crm.hr.entity.LeaveRequest;
import org.springframework.stereotype.Component;

@Component
public class LeaveMapper {

    public LeaveResponse toResponse(LeaveRequest leaveRequest) {
        if (leaveRequest == null) {
            return null;
        }

        LeaveResponse response = new LeaveResponse();
        response.setId(leaveRequest.getId());
        response.setEmployeeId(leaveRequest.getEmployeeId());
        response.setStartDate(leaveRequest.getStartDate());
        response.setEndDate(leaveRequest.getEndDate());
        response.setReason(leaveRequest.getReason());
        response.setStatus(leaveRequest.getStatus() != null ? leaveRequest.getStatus().name() : null);
        response.setCreatedAt(leaveRequest.getCreatedAt());
        response.setUpdatedAt(leaveRequest.getUpdatedAt());

        return response;
    }

    public LeaveRequest toEntity(LeaveRequestDto request) {
        if (request == null) {
            return null;
        }

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployeeId(request.getEmployeeId());
        leaveRequest.setStartDate(request.getStartDate());
        leaveRequest.setEndDate(request.getEndDate());
        leaveRequest.setReason(request.getReason());
        leaveRequest.setStatus(LeaveRequest.LeaveStatus.PENDING);

        return leaveRequest;
    }

    public void updateEntityFromRequest(LeaveRequest leaveRequest, LeaveRequestDto request) {
        if (request == null) {
            return;
        }
        leaveRequest.setEmployeeId(request.getEmployeeId());
        leaveRequest.setStartDate(request.getStartDate());
        leaveRequest.setEndDate(request.getEndDate());
        leaveRequest.setReason(request.getReason());
    }
}
