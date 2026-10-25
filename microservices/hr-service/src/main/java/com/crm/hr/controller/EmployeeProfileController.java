package com.crm.hr.controller;

import com.crm.hr.dto.EmployeeProfileSummary;
import com.crm.hr.exception.EmployeeProfileException;
import com.crm.hr.service.EmployeeProfileService;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employee-profile")
public class EmployeeProfileController {
    private final EmployeeProfileService profiles;
    public EmployeeProfileController(EmployeeProfileService profiles) { this.profiles = profiles; }
    @GetMapping("/me")
    public ResponseEntity<EmployeeProfileSummary> me(Authentication authentication, @RequestParam Map<String, String> parameters) {
        if (!parameters.isEmpty()) throw new EmployeeProfileException(400, "PROFILE_INVALID_REQUEST");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(profiles.own(authentication));
    }
    public record ProfileError(int status, String code, String message) {}
    @ExceptionHandler(EmployeeProfileException.class)
    public ResponseEntity<ProfileError> failure(EmployeeProfileException ex) {
        return ResponseEntity.status(ex.status()).cacheControl(CacheControl.noStore())
                .body(new ProfileError(ex.status(), ex.code(), "Employee profile request could not be completed"));
    }
}
