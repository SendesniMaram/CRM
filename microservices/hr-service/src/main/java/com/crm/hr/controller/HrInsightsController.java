package com.crm.hr.controller;
import com.crm.hr.service.*;import java.time.LocalDate;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;import jakarta.servlet.http.HttpServletRequest;
@RestController public class HrInsightsController {
 private final LeaveBalanceService balances;private final HrStatisticsService statistics;public HrInsightsController(LeaveBalanceService b,HrStatisticsService s){balances=b;statistics=s;}
 @GetMapping("/api/leaves/balance/me") public ResponseEntity<LeaveBalanceCalculator.Balance> own(@RequestParam LocalDate from,@RequestParam LocalDate to,HttpServletRequest request){if(request.getParameterMap().keySet().stream().anyMatch(k->!java.util.Set.of("from","to").contains(k)))throw new IllegalArgumentException("Personal target filters are forbidden");return noStore(balances.own(from,to));}
 @GetMapping("/api/leaves/balance/employees/{employee}") public ResponseEntity<LeaveBalanceCalculator.Balance> admin(@PathVariable String employee,@RequestParam LocalDate from,@RequestParam LocalDate to,@RequestHeader(HttpHeaders.AUTHORIZATION) String bearer){return noStore(balances.administration(employee,from,to,bearer));}
 @GetMapping("/api/hr-insights") public ResponseEntity<HrStatisticsService.Summary> statistics(@RequestParam LocalDate from,@RequestParam LocalDate to){return noStore(statistics.read(from,to));}
 private <T> ResponseEntity<T> noStore(T body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
}