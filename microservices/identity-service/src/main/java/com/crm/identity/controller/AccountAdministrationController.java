package com.crm.identity.controller;
import com.crm.identity.service.AccountAdministrationService;import com.crm.identity.entity.User;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.http.*;import jakarta.validation.Valid;import java.time.LocalDate;
@RestController @RequestMapping("/api/users")
public class AccountAdministrationController {
 private final AccountAdministrationService service;public AccountAdministrationController(AccountAdministrationService s){service=s;}
 @GetMapping("/directory") public ResponseEntity<?> directory(@AuthenticationPrincipal User actor,@RequestParam(required=false) String keyword,@RequestParam(required=false) String role,@RequestParam(required=false) String state,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ok(service.directory(actor,keyword,role,state,page,size));}
 @GetMapping("/display-settings") public ResponseEntity<?> display(){return ok(service.display());}
 @GetMapping("/administration/settings") public ResponseEntity<?> settings(@AuthenticationPrincipal User actor){return ok(service.settings(actor));}
 @PutMapping("/administration/settings") public ResponseEntity<?> settings(@AuthenticationPrincipal User actor,@Valid @RequestBody AccountAdministrationService.SettingsInput input){return ok(service.updateSettings(actor,input));}
 @GetMapping("/administration/audit") public ResponseEntity<?> audit(@AuthenticationPrincipal User actor,@RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam(required=false) String action,@RequestParam(required=false) Long target,@RequestParam(required=false) Long author,@RequestParam(defaultValue="0") int page){return ok(service.audit(actor,from,to,action,target,author,page));}
 @GetMapping("/administration/audit/{id}") public ResponseEntity<?> event(@AuthenticationPrincipal User actor,@PathVariable long id){return ok(service.event(actor,id));}
 @PostMapping("/administration/users/{id}/revoke-sessions") public ResponseEntity<?> revoke(@AuthenticationPrincipal User actor,@PathVariable long id){service.revoke(actor,id);return ok(java.util.Map.of("revoked",true));}
 private ResponseEntity<?> ok(Object value){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value);}
}
