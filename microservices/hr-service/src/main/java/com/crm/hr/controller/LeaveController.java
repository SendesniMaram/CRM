package com.crm.hr.controller;

import com.crm.hr.dto.LeaveRequestDto;
import com.crm.hr.dto.LeaveResponse;
import com.crm.hr.service.ILeaveService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/leaves", produces = MediaType.APPLICATION_JSON_VALUE)
public class LeaveController {

    private final ILeaveService leaveService;

    public LeaveController(ILeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping
    public ResponseEntity<LeaveResponse> createLeave(@Valid @RequestBody LeaveRequestDto request) {
        LeaveResponse response = leaveService.createLeave(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllLeaves(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String keyword) {

        if (page == 0 && size == 10 && sortBy == null && direction.equals("asc") && keyword == null) {
            List<LeaveResponse> leaves = leaveService.getAllLeaves();
            return ResponseEntity.ok(leaves);
        }

        Page<LeaveResponse> leavePage = leaveService.getAllLeavesPaged(page, size, sortBy, direction, keyword);
        return ResponseEntity.ok(leavePage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveResponse> getLeaveById(@PathVariable Long id) {
        LeaveResponse response = leaveService.getLeaveById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeaveResponse> updateLeave(
            @PathVariable Long id,
            @Valid @RequestBody LeaveRequestDto request) {
        LeaveResponse response = leaveService.updateLeave(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<LeaveResponse> approveLeave(@PathVariable Long id) {
        LeaveResponse response = leaveService.approveLeave(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<LeaveResponse> rejectLeave(@PathVariable Long id) {
        LeaveResponse response = leaveService.rejectLeave(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLeave(@PathVariable Long id) {
        leaveService.deleteLeave(id);
        return ResponseEntity.noContent().build();
    }
}
