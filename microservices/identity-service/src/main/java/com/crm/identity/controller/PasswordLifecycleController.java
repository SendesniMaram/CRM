package com.crm.identity.controller;
import com.crm.identity.service.PasswordLifecycleService;import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import jakarta.validation.Valid;
@RestController public class PasswordLifecycleController {private final PasswordLifecycleService service;public PasswordLifecycleController(PasswordLifecycleService s){service=s;}
 @PostMapping("/api/auth/password") public ResponseEntity<Void> change(@Valid @RequestBody PasswordLifecycleService.Input input){service.change(input);return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();}
}
