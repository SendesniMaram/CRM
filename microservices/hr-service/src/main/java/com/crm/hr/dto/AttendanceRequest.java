package com.crm.hr.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRequest {

    @NotBlank(message = "Employee ID is required")
    @Size(max = 50, message = "Employee ID must be at most 50 characters")
    private String employeeId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    private LocalTime checkIn;

    private LocalTime checkOut;

    @Size(max = 20, message = "Status must be at most 20 characters")
    private String status;

    @Size(max = 500, message = "Notes must be at most 500 characters")
    private String notes;
}
