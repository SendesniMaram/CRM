package com.crm.fees.controller;

import com.crm.fees.dto.FeeRequest;
import com.crm.fees.dto.FeeResponse;
import com.crm.fees.service.IFeeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/fees", produces = MediaType.APPLICATION_JSON_VALUE)
public class FeeController {

    private final IFeeService feeService;

    public FeeController(IFeeService feeService) {
        this.feeService = feeService;
    }

    @PostMapping
    public ResponseEntity<FeeResponse> createFee(@Valid @RequestBody FeeRequest request) {
        FeeResponse response = feeService.createFee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllFees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String keyword) {

        if (page == 0 && size == 10 && sortBy == null && direction.equals("asc") && keyword == null) {
            List<FeeResponse> fees = feeService.getAllFees();
            return ResponseEntity.ok(fees);
        }

        Page<FeeResponse> feePage = feeService.getAllFeesPaged(page, size, sortBy, direction, keyword);
        return ResponseEntity.ok(feePage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeeResponse> getFeeById(@PathVariable Long id) {
        FeeResponse response = feeService.getFeeById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FeeResponse> updateFee(
            @PathVariable Long id,
            @Valid @RequestBody FeeRequest request) {
        FeeResponse response = feeService.updateFee(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFee(@PathVariable Long id) {
        feeService.deleteFee(id);
        return ResponseEntity.noContent().build();
    }
}
