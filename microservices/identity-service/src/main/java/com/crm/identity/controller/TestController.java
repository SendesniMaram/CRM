package com.crm.identity.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class TestController {

    @GetMapping("/test")
    public Map<String, Object> test() {
        return Map.of(
                "service", "identity-service",
                "status", "running"
        );
    }
}


