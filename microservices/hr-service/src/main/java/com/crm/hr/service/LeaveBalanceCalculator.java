package com.crm.hr.service;
import com.crm.hr.entity.*;import com.crm.hr.repository.LeaveBalanceRepository.RequestDays;import java.math.BigDecimal;import java.time.LocalDate;import java.time.temporal.ChronoUnit;import java.util.*;
/** Technical calendar-day union. No acquisition, carry-over or legal entitlement. */
public final class LeaveBalanceCalculator {
 private LeaveBalanceCalculator(){}
 public record Detail(String id,LocalDate from,LocalDate to,String status,long daysInPeriod){}
 public record Quota(String id,LocalDate from,LocalDate to,String type,BigDecimal days){}
 public record Balance(String employeeId,LocalDate from,LocalDate to,String policy,String state,BigDecimal grantedDays,long consumedDays,long pendingDays,BigDecimal remainingDays,BigDecimal availableAfterPending,List<Quota> quotas,List<Detail> requests,boolean overlapsDetected){}
 private record Interval(LocalDate from,LocalDate to){}
 public static Balance calculate(String employee,LocalDate from,LocalDate to,List<LeaveQuota> quotas,List<RequestDays> rows){
  HrPeriod.require(from,to);List<Interval> approved=new ArrayList<>(),pending=new ArrayList<>();List<Detail> details=new ArrayList<>();
  for(var row:rows){if(row.getStartDate()==null||row.getEndDate()==null||row.getStatus()==null||row.getEndDate().isBefore(row.getStartDate()))throw new IllegalArgumentException("Inconsistent historical leave dates");
   LocalDate start=row.getStartDate().isBefore(from)?from:row.getStartDate(),end=row.getEndDate().isAfter(to)?to:row.getEndDate();if(end.isBefore(start))continue;
   long days=ChronoUnit.DAYS.between(start,end)+1;details.add(new Detail(String.valueOf(row.getId()),start,end,row.getStatus().name(),days));
   if(row.getStatus()==LeaveRequest.LeaveStatus.APPROVED)approved.add(new Interval(start,end));else if(row.getStatus()==LeaveRequest.LeaveStatus.PENDING)pending.add(new Interval(start,end));}
  long consumed=count(approved);List<Interval> all=new ArrayList<>(approved);all.addAll(pending);long reserved=count(all)-consumed;
  long raw=all.stream().mapToLong(i->ChronoUnit.DAYS.between(i.from,i.to)+1).sum();
  BigDecimal granted=quotas.isEmpty()?null:quotas.stream().map(LeaveQuota::getGrantedDays).reduce(BigDecimal.ZERO,BigDecimal::add);
  boolean exact=quotas.stream().allMatch(q->from.equals(q.getPeriodStart())&&to.equals(q.getPeriodEnd()));
  String state=quotas.isEmpty()?"NO_QUOTA":!exact?"QUOTA_PERIOD_MISMATCH":quotas.size()!=1?"QUOTA_TYPE_AMBIGUOUS":"TECHNICAL_BALANCE";
  BigDecimal remaining=state.equals("TECHNICAL_BALANCE")?granted.subtract(BigDecimal.valueOf(consumed)):null;
  List<Quota> info=quotas.stream().map(q->new Quota(String.valueOf(q.getId()),q.getPeriodStart(),q.getPeriodEnd(),q.getLeaveType(),q.getGrantedDays())).toList();
  return new Balance(employee,from,to,"CALENDAR_DAYS_ALL_LEAVE_REQUESTS_NON_LEGAL",state,granted,consumed,reserved,remaining,remaining==null?null:remaining.subtract(BigDecimal.valueOf(reserved)),info,List.copyOf(details),raw>count(all));}
 private static long count(List<Interval> source){var sorted=source.stream().sorted(Comparator.comparing(Interval::from)).toList();if(sorted.isEmpty())return 0;LocalDate start=sorted.getFirst().from,end=sorted.getFirst().to;long days=0;
  for(int i=1;i<sorted.size();i++){var next=sorted.get(i);if(!next.from.isAfter(end.plusDays(1))){if(next.to.isAfter(end))end=next.to;}else{days+=ChronoUnit.DAYS.between(start,end)+1;start=next.from;end=next.to;}}return days+ChronoUnit.DAYS.between(start,end)+1;}
}