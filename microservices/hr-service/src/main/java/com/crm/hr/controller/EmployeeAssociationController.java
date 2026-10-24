package com.crm.hr.controller;

import com.crm.hr.dto.EmployeeAssociationRequest;
import com.crm.hr.entity.LeaveEmployeeAssociation;
import com.crm.hr.service.EmployeeAssociationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employee-associations")
public class EmployeeAssociationController {
    private final EmployeeAssociationService associations;

    public EmployeeAssociationController(EmployeeAssociationService associations) {
        this.associations = associations;
    }

    public record AssociationResponse(String userId, String employeeId) {
        static AssociationResponse from(LeaveEmployeeAssociation link) {
            return new AssociationResponse(link.getUserId().toString(), link.getEmployeeId());
        }
    }

    @PostMapping
    public ResponseEntity<AssociationResponse> create(@Valid @RequestBody EmployeeAssociationRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearer) {
        var result = associations.create(EmployeeAssociationService.parseId(request.userId()),
                EmployeeAssociationService.parseId(request.employeeId()), bearer);
        var body = AssociationResponse.from(result.association());
        return (result.created() ? ResponseEntity.created(URI.create("/api/employee-associations/users/" + body.userId()))
                : ResponseEntity.ok()).cacheControl(CacheControl.noStore()).body(body);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<AssociationResponse> read(@PathVariable String userId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(AssociationResponse.from(
                associations.readForAdministration(EmployeeAssociationService.parseId(userId))));
    }

    @GetMapping("/me")
    public ResponseEntity<AssociationResponse> me(Authentication authentication, @RequestParam Map<String, String> parameters) {
        if (!parameters.isEmpty()) throw new IllegalArgumentException("Personal association does not accept target parameters");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
                AssociationResponse.from(associations.readOwn(authentication)));
    }
}
