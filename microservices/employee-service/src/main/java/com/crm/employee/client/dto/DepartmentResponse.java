package com.crm.employee.client.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponse {

    private Long id;

    private String name;

    private String description;

    private String code;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

