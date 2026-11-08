package com.crm.payroll.service;
import com.crm.payroll.repository.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayrollTechnicalExport {
 private final PayrollRepository payrolls;private final PayrollSettlementRepository entries;
 public PayrollTechnicalExport(PayrollRepository p,PayrollSettlementRepository e){payrolls=p;entries=e;}
 @Transactional(readOnly=true)
 public byte[] csv(Authentication auth,String period){
  PayrollAuthorization.admin(auth);
  if(period==null||!period.matches("[0-9]{4}-(0[1-9]|1[0-2])"))throw new IllegalArgumentException("Period must be YYYY-MM");YearMonth.parse(period);
  StringBuilder out=new StringBuilder("\uFEFF");
  out.append("classification,period,payroll_reference,employee_reference,currency,base_salary,bonuses,overtime_hours,overtime_hourly_rate,overtime_amount,deductions,entered_taxes,net_salary,status,published,cancelled,settled_amount,remaining_amount,settlement_state,correction_of,replaced_by\r\n");
  int number=0;Page<com.crm.payroll.entity.Payroll> page;
  do{
   page=payrolls.findByPayPeriod(period,PageRequest.of(number++,250,Sort.by("id")));
   if(page.getTotalElements()>5000)throw new IllegalArgumentException("Technical export is limited to 5000 payrolls per period");
   for(var row:page){
    var history=entries.findByPayrollIdOrderByCreatedAtAscIdAsc(row.getId());BigDecimal paid=PayrollLedger.paid(history),remaining=PayrollLedger.remaining(row,history);
    String state=row.isCancelled()?"CANCELLED":remaining==null?"LEGACY_UNRECONCILED":row.getNetSalary().signum()==0?"NO_AMOUNT_DUE":remaining.signum()==0?"SETTLED":paid.signum()>0?"PARTIAL":"UNPAID";
    Object[] values={"TECHNICAL_NON_REGULATORY",row.getPayPeriod(),row.getId(),row.getEmployeeId(),row.getCurrency(),row.getBaseSalary(),row.getBonuses(),row.getOvertimeHours(),row.getOvertimeHourlyRate(),row.getOvertimeAmount(),row.getDeductions(),row.getTaxAmount(),row.getNetSalary(),row.getStatus(),row.isPublished(),row.isCancelled(),paid,remaining,state,row.getCorrectionOfId(),row.getReplacementPayrollId()};
    for(int i=0;i<values.length;i++){if(i>0)out.append(',');out.append(cell(values[i]));}out.append("\r\n");
   }
  }while(page.hasNext());
  return out.toString().getBytes(StandardCharsets.UTF_8);
 }
 public static String cell(Object value){
  String text=value==null?"":value instanceof BigDecimal n?n.toPlainString():value.toString();
  if(!text.isEmpty()&&(Character.isWhitespace(text.charAt(0))||"=+-@".indexOf(text.charAt(0))>=0))text="'"+text;
  return "\""+text.replace("\"","\"\"")+"\"";
 }
}
