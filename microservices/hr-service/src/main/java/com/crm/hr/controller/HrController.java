package com.crm.hr.controller;

import com.crm.hr.dto.HrRequest;
import com.crm.hr.dto.HrResponse;
import com.crm.hr.service.IHrService;
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
@RequestMapping(path = "/api/hr-records", produces = MediaType.APPLICATION_JSON_VALUE)
public class HrController {

    private final IHrService hrService;

    public HrController(IHrService hrService) {
        this.hrService = hrService;
    }

    @PostMapping
    public ResponseEntity<HrResponse> createRecord(@Valid @RequestBody HrRequest request) {
        HrResponse response = hrService.createRecord(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String keyword) {

        if (page == 0 && size == 10 && sortBy == null && direction.equals("asc") && keyword == null) {
            List<HrResponse> records = hrService.getAllRecords();
            return ResponseEntity.ok(records);
        }

        Page<HrResponse> recordPage = hrService.getAllRecordsPaged(page, size, sortBy, direction, keyword);
        return ResponseEntity.ok(recordPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HrResponse> getRecordById(@PathVariable Long id) {
        HrResponse response = hrService.getRecordById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HrResponse> updateRecord(
            @PathVariable Long id,
            @Valid @RequestBody HrRequest request) {
        HrResponse response = hrService.updateRecord(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id) {
        hrService.deleteRecord(id);
        return ResponseEntity.noContent().build();
    }
}
