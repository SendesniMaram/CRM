package com.crm.hr.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrResponse {

    private Long id;

    private String employeeId;

    private String contractType;

    private LocalDateTime hireDate;

    private String jobTitle;

    private String salaryGrade;

    private String manager;

    private String workLocation;

    private String employmentStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
