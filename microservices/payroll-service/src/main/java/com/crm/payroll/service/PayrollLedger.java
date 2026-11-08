package com.crm.payroll.service;
import com.crm.payroll.entity.*;
import java.math.BigDecimal;
import java.util.List;
public final class PayrollLedger {
 private PayrollLedger(){}
 public static BigDecimal paid(List<PayrollSettlement> rows){return rows.stream().map(e->"REVERSAL".equals(e.getKind())?e.getAmount().negate():e.getAmount()).reduce(BigDecimal.ZERO,BigDecimal::add);}
 public static boolean legacy(Payroll payroll,List<PayrollSettlement> rows){return "PAID".equals(payroll.getStatus())&&rows.isEmpty();}
 public static BigDecimal remaining(Payroll payroll,List<PayrollSettlement> rows){return legacy(payroll,rows)?null:payroll.isCancelled()?BigDecimal.ZERO:payroll.getNetSalary().subtract(paid(rows));}
}
