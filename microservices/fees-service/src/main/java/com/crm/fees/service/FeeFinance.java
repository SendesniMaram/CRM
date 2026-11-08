package com.crm.fees.service;
import java.math.*;import jakarta.validation.Validation;import jakarta.validation.Validator;
public final class FeeFinance {
 private FeeFinance(){} private static final Validator VALIDATOR=Validation.buildDefaultValidatorFactory().getValidator();public static final BigDecimal MAX=new BigDecimal("9999999999.99");
 public static void validate(Object value){if(value==null||!VALIDATOR.validate(value).isEmpty())throw new IllegalArgumentException("Invalid fee data, precision or range");}
 public static BigDecimal amount(BigDecimal rate,BigDecimal hours){if(rate==null||hours==null||rate.signum()<=0||hours.signum()<=0||rate.scale()>2||hours.scale()>2||rate.compareTo(MAX)>0||hours.compareTo(new BigDecimal("999999.99"))>0)throw new IllegalArgumentException("Positive rate and hours with at most two decimals required");
  BigDecimal amount=rate.multiply(hours).setScale(2,RoundingMode.HALF_UP);if(amount.signum()<=0||amount.compareTo(MAX)>0)throw new IllegalArgumentException("Calculated amount must be positive and fit DECIMAL(12,2)");return amount;}
 public static org.springframework.web.server.ResponseStatusException conflict(){return new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Fee state, tariff or duplicate conflicts with this operation");}
}