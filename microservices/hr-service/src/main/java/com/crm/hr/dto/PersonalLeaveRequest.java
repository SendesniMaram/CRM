package com.crm.hr.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PersonalLeaveRequest(
        @NotNull @FutureOrPresent LocalDate startDate,
        @NotNull @FutureOrPresent LocalDate endDate,
        @Size(max = 500) String reason) {
    public LeaveRequestDto forEmployee(String employeeId) {
        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setEmployeeId(employeeId);
        dto.setStartDate(startDate);
        dto.setEndDate(endDate);
        dto.setReason(reason);
        return dto;
    }
}
