package com.crm.customer.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {

    private Long id;

    private String name;

    private String email;

    private String phone;

    private String company;

    private String address;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

