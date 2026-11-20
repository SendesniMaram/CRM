package com.crm.hr.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingResponse {

    private Long id;

    private String employeeId;

    private String title;

    private String provider;

    private LocalDate startDate;

    private LocalDate endDate;

    private Double cost;

    private String status;

    private Boolean certificateObtained;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
