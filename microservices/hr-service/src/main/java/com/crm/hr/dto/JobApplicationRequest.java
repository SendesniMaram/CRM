package com.crm.hr.dto;
import com.crm.hr.entity.JobApplication;
import jakarta.validation.constraints.*;
public record JobApplicationRequest(@NotNull @Positive Long offerId,@NotBlank @Size(max=200) String candidateName,@NotBlank @Email @Size(max=254) String candidateEmail,@Size(max=10000) String notes,@NotNull JobApplication.Status status) {}
