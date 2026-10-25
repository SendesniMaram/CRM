package com.crm.identity.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authentication and current-state checks are performed by the dedicated security chain. */
@RestController
public class AccessTokenValidationController {
    public static final String PATH = "/api/internal/access-token/validate";

    @GetMapping(PATH)
    public ResponseEntity<Void> validate() {
        return ResponseEntity.noContent().header("Cache-Control", "no-store").build();
    }
}
