package com.crm.hr.dto;
import com.crm.hr.entity.JobOffer;
import jakarta.validation.constraints.*;
public record JobOfferRequest(@NotBlank @Size(max=200) String title,@Size(max=10000) String description,@NotNull JobOffer.Status status) {}
