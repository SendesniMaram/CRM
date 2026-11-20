package com.crm.hr.dto;
import com.crm.hr.entity.CareerEvent; import jakarta.validation.constraints.*; import java.time.LocalDate;
public record CareerEventRequest(@NotBlank @Pattern(regexp="[1-9][0-9]{0,18}") String employeeId,@NotNull LocalDate effectiveDate,@NotNull CareerEvent.Type type,@Size(max=200) String previousValue,@NotBlank @Size(max=200) String newValue,@Size(max=2000) String comment){}
