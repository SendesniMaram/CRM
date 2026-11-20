package com.crm.invoice.dto;
import jakarta.validation.constraints.*;
 public record InvoiceReminderRequest(@NotBlank @Pattern(regexp="MANUAL|PHONE|EMAIL_RECORDED") String channel, @Size(max=500) String notes) { }
