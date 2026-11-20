package com.crm.payroll.service;
import com.crm.payroll.dto.PayrollRequest;
import java.math.*;
public final class PayrollCalculation {
 private PayrollCalculation(){}
 public static BigDecimal overtime(PayrollRequest request){
  FinancialValidation.request(request);
  BigDecimal hours=request.getOvertimeHours(),rate=request.getOvertimeHourlyRate();
  if(hours==null&&rate==null)return BigDecimal.ZERO;
  if(hours==null||rate==null||hours.signum()<0||rate.signum()<0||hours.signum()>0&&rate.signum()==0)
   throw new IllegalArgumentException("Overtime hours and explicitly applicable rate are required together");
  BigDecimal result=hours.multiply(rate).setScale(2,RoundingMode.HALF_UP);
  FinancialValidation.calculated(result);return result;
 }
 public static BigDecimal net(PayrollRequest request){
  FinancialValidation.request(request);
  BigDecimal result=request.getBaseSalary().add(zero(request.getBonuses())).add(overtime(request))
    .subtract(zero(request.getDeductions())).subtract(zero(request.getTaxAmount()));
  FinancialValidation.calculated(result);return result;
 }
 private static BigDecimal zero(BigDecimal value){return value==null?BigDecimal.ZERO:value;}
}
