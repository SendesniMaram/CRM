package com.crm.hr.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {

    private Long id;

    private String employeeId;

    private LocalDate date;

    private LocalTime checkIn;

    private LocalTime checkOut;

    private String status;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
