package com.crm.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequest {

    @NotBlank(message = "Customer name is required")
    @Size(max = 255, message = "Customer name must be at most 255 characters")
    private String name;

    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    @Size(max = 50, message = "Phone must be at most 50 characters")
    @Pattern(regexp = "^(\\+\\d{1,3}[- ]?)?\\d{6,14}$", message = "Phone number must be valid (e.g., +123456789)")
    private String phone;

    @Size(max = 255, message = "Company must be at most 255 characters")
    private String company;

    @Size(max = 500, message = "Address must be at most 500 characters")
    private String address;
}

