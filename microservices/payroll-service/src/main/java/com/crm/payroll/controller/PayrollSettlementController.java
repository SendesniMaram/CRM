package com.crm.payroll.controller;
import com.crm.payroll.service.*;
import com.crm.payroll.service.impl.PayrollServiceImpl;
import com.crm.payroll.dto.PayrollResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class PayrollSettlementController {
 private final PayrollSettlementService settlements;private final PayrollServiceImpl payrolls;private final PayrollTechnicalExport export;
 public PayrollSettlementController(PayrollSettlementService s,PayrollServiceImpl p,PayrollTechnicalExport e){settlements=s;payrolls=p;export=e;}
 public record CorrectionRequest(@NotBlank @Size(max=500) String reason){}
 @GetMapping("/api/payrolls/{id}/settlements")
 public ResponseEntity<PayrollSettlementService.Summary> list(Authentication auth,@PathVariable long id){return response(settlements.administration(auth,id));}
 @PostMapping("/api/payrolls/{id}/settlements")
 public ResponseEntity<PayrollSettlementService.Summary> pay(Authentication auth,@PathVariable long id,@Valid @RequestBody PayrollSettlementService.PaymentRequest request){return response(settlements.pay(auth,id,request));}
 @PostMapping("/api/payrolls/{id}/settlements/{reference}/reversal")
 public ResponseEntity<PayrollSettlementService.Summary> reverse(Authentication auth,@PathVariable long id,@PathVariable String reference,@Valid @RequestBody PayrollSettlementService.ReversalRequest request){return response(settlements.reverse(auth,id,reference,request));}
 @GetMapping("/api/payrolls/me/{id}/settlements")
 public ResponseEntity<PayrollSettlementService.Summary> own(Authentication auth,@RequestHeader("Authorization") String bearer,@PathVariable long id,HttpServletRequest request){
  if(!request.getParameterMap().isEmpty())throw new IllegalArgumentException("Target parameters are not allowed");return response(settlements.own(auth,bearer,id));
 }
 @PostMapping("/api/payrolls/{id}/corrections")
 public ResponseEntity<PayrollResponse> correct(Authentication auth,@PathVariable long id,@Valid @RequestBody CorrectionRequest request){return response(payrolls.correct(auth,id,request.reason()));}
 @GetMapping(value="/api/payrolls/export/technical.csv",produces="text/csv")
 public ResponseEntity<byte[]> csv(Authentication auth,@RequestParam String period){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=payroll-technical-"+validPeriod(period)+".csv").contentType(new MediaType("text","csv",java.nio.charset.StandardCharsets.UTF_8)).body(export.csv(auth,period));}
 private String validPeriod(String period){if(period==null||!period.matches("[0-9]{4}-(0[1-9]|1[0-2])"))throw new IllegalArgumentException("Invalid period");return period;}
 private <T> ResponseEntity<T> response(T body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
}
