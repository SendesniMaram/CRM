package com.crm.customer.controller;

import com.crm.customer.dto.CustomerRequest;
import com.crm.customer.dto.CustomerResponse;
import com.crm.customer.service.ICustomerService;
import com.crm.customer.service.CustomerCsvService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import com.crm.customer.client.CustomerEmployeeDirectory;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final ICustomerService customerService;
    private final CustomerCsvService csvService;
    @Autowired(required = false) private CustomerEmployeeDirectory employeeDirectory;

    public CustomerController(ICustomerService customerService, CustomerCsvService csvService) {
        this.customerService = customerService;
        this.csvService = csvService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request, @org.springframework.web.bind.annotation.RequestHeader(value=HttpHeaders.AUTHORIZATION, required=false) String authorization) {
        validateEmployee(request, authorization);
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String contactType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String segment,
            @RequestParam(required = false) Long assignedEmployeeId) {

        if (page == 0 && size == 10 && sortBy == null && direction.equals("asc") && keyword == null && contactType == null && category == null && segment == null && assignedEmployeeId == null) {
            List<CustomerResponse> customers = customerService.getAllCustomers();
            return ResponseEntity.ok(customers);
        }

        Page<CustomerResponse> customerPage = contactType == null && category == null && segment == null && assignedEmployeeId == null
                ? customerService.getAllCustomersPaged(page, size, sortBy, direction, keyword)
                : customerService.getAllCustomersPaged(page, size, sortBy, direction, keyword, contactType, category, segment, assignedEmployeeId);
        return ResponseEntity.ok(customerPage);
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<CustomerResponse> convertLead(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.convertLead(id));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CustomerCsvService.ImportReport> importCsv(@org.springframework.web.bind.annotation.RequestPart("file") MultipartFile file,
            @org.springframework.web.bind.annotation.RequestHeader(value=HttpHeaders.AUTHORIZATION, required=false) String authorization) {
        if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV file is empty");
        if (file.getSize() > 5 * 1024 * 1024) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "CSV file exceeds 5 MiB");
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(java.util.Locale.ROOT).endsWith(".csv")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload a .csv file");
        }
        try {
            return ResponseEntity.ok(csvService.importCsv(file.getBytes(), request -> {
                validateEmployee(request, authorization);
                return customerService.createCustomer(request);
            }));
        } catch (java.io.IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV file could not be read");
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String contactType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String segment,
            @RequestParam(required = false) Long assignedEmployeeId) {
        byte[] content = csvService.exportCsv(keyword, contactType, category, segment, assignedEmployeeId);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("contacts-crm.csv", java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(content);
    }

    @GetMapping(value = "/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String contactType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String segment,
            @RequestParam(required = false) Long assignedEmployeeId) {
        byte[] content = csvService.exportXlsx(keyword, contactType, category, segment, assignedEmployeeId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("contacts-crm.xlsx").build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(content);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        CustomerResponse response = customerService.getCustomerById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request,
            @org.springframework.web.bind.annotation.RequestHeader(value=HttpHeaders.AUTHORIZATION, required=false) String authorization) {
        validateEmployee(request, authorization);
        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    private void validateEmployee(CustomerRequest request, String authorization) {
        if(request.getAssignedEmployeeId()==null) return;
        if(employeeDirectory==null || authorization==null || !authorization.startsWith("Bearer "))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Employee association could not be verified");
        employeeDirectory.requireExisting(request.getAssignedEmployeeId(),authorization);
    }
}
