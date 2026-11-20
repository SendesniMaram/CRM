package com.crm.fees.dto;
import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.time.LocalDate;
public final class FeeCycleContracts {private FeeCycleContracts(){}
 public record OfferingInput(@NotBlank @Size(max=255) String title,@NotBlank @Pattern(regexp="[1-9][0-9]{0,18}") String employeeId,@Pattern(regexp="[1-9][0-9]{0,18}") String customerId,@NotBlank @Pattern(regexp="[A-Z]{3}") String currency){}
 public record TariffInput(@NotNull @Positive @Digits(integer=10,fraction=2) BigDecimal rate,@NotNull LocalDate validFrom,LocalDate validTo,@NotNull Boolean active){}
 public record TimeInput(@NotNull LocalDate workDate,@NotNull @Positive @Digits(integer=6,fraction=2) BigDecimal hours,@NotBlank @Size(max=500) String description){}
 public record NewTime(@NotBlank @Size(min=36,max=36) String requestId,@NotNull LocalDate workDate,@NotNull @Positive @Digits(integer=6,fraction=2) BigDecimal hours,@NotBlank @Size(max=500) String description){public TimeInput value(){return new TimeInput(workDate,hours,description);}}
 public record Activation(@NotNull Boolean active){}
 public record Cancellation(@NotBlank @Size(max=500) String reason){}
 public record OfferingView(String id,String title,String employeeId,String customerId,String currency,boolean active){}
 public record TariffView(String id,String offeringId,BigDecimal rate,LocalDate validFrom,LocalDate validTo,boolean active,boolean used){}
 public record TimeView(String id,String offeringId,LocalDate workDate,BigDecimal hours,String description,String tariffId,BigDecimal appliedRate,BigDecimal amount,String currency,String feeId,boolean cancelled,String cancelReason,String createdBy,java.time.LocalDateTime createdAt){}
 public record Posted(String feeId){}
}