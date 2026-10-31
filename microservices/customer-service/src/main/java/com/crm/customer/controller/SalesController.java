package com.crm.customer.controller;

import com.crm.customer.client.CustomerEmployeeDirectory;
import com.crm.customer.entity.CustomerInteraction;
import com.crm.customer.entity.SalesOpportunity;
import com.crm.customer.entity.OpportunityStageHistory;
import com.crm.customer.repository.CustomerInteractionRepository;
import com.crm.customer.repository.CustomerRepository;
import com.crm.customer.repository.OpportunityStageHistoryRepository;
import com.crm.customer.repository.SalesOpportunityRepository;
import com.crm.customer.util.SalesOpportunitySpecification;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Validated
public class SalesController {
    private static final Set<String> STAGES = Set.of(
            "NEW", "QUALIFICATION", "PROPOSAL", "NEGOTIATION", "WON", "LOST");
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "title", "stage", "estimatedAmount", "expectedCloseDate", "createdAt");

    private final CustomerRepository customers;
    private final CustomerInteractionRepository interactions;
    private final SalesOpportunityRepository opportunities;
    private final OpportunityStageHistoryRepository stageHistory;
    private final CustomerEmployeeDirectory employeeDirectory;

    public SalesController(CustomerRepository customers,
                           CustomerInteractionRepository interactions,
                           SalesOpportunityRepository opportunities,
                           OpportunityStageHistoryRepository stageHistory,
                           CustomerEmployeeDirectory employeeDirectory) {
        this.customers = customers;
        this.interactions = interactions;
        this.opportunities = opportunities;
        this.stageHistory = stageHistory;
        this.employeeDirectory = employeeDirectory;
    }

    public record InteractionRequest(
            @NotBlank @Pattern(regexp = "CALL|EMAIL|MEETING|NOTE") String interactionType,
            @NotNull LocalDateTime occurredAt,
            Long employeeId,
            @Size(max = 5000) String notes) { }

    public record OpportunityRequest(
            @NotNull @Positive Long customerId,
            Long employeeId,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 1000) String description,
            @PositiveOrZero BigDecimal estimatedAmount,
            @Pattern(regexp = "NEW|QUALIFICATION|PROPOSAL|NEGOTIATION|WON|LOST") String stage,
            LocalDate expectedCloseDate) { }

    public record OpportunityStageRequest(
            @NotBlank @Pattern(regexp = "NEW|QUALIFICATION|PROPOSAL|NEGOTIATION|WON|LOST") String stage) { }

    @GetMapping("/api/customers/{customerId}/interactions")
    public List<CustomerInteraction> interactions(@PathVariable Long customerId) {
        requireCustomer(customerId);
        return interactions.findByCustomerIdOrderByOccurredAtDesc(customerId);
    }

    @PostMapping("/api/customers/{customerId}/interactions")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerInteraction addInteraction(
            @PathVariable Long customerId,
            @Valid @RequestBody InteractionRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String bearer) {
        requireEmployee(request.employeeId(), bearer);
        requireCustomer(customerId);
        var row = new CustomerInteraction();
        row.setCustomerId(customerId);
        row.setEmployeeId(request.employeeId());
        row.setInteractionType(request.interactionType());
        row.setOccurredAt(request.occurredAt());
        row.setNotes(request.notes());
        return interactions.save(row);
    }

    @GetMapping("/api/opportunities")
    public ResponseEntity<?> opportunities(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @PositiveOrZero Integer page,
            @RequestParam(required = false) @Positive Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? 10 : size;
        if (pageSize > 100 || !SORT_FIELDS.contains(sortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid opportunity page options");
        }
        if (stage != null && !STAGES.contains(stage)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported opportunity stage");
        }
        var sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        var filters = SalesOpportunitySpecification.filters(keyword, stage, employeeId, customerId);
        var sort = Sort.by(sortDirection, sortBy);
        if (page == null && size == null) {
            return ResponseEntity.ok(opportunities.findAll(filters, sort));
        }
        return ResponseEntity.ok(opportunities.findAll(filters, PageRequest.of(pageNumber, pageSize, sort)));
    }

    @GetMapping("/api/opportunities/{id}")
    public SalesOpportunity getOpportunity(@PathVariable Long id) {
        return findOpportunity(id);
    }

    @GetMapping("/api/opportunities/{id}/history")
    public List<OpportunityStageHistory> history(@PathVariable Long id) {
        findOpportunity(id);
        return stageHistory.findByOpportunityIdOrderByChangedAtDescIdDesc(id);
    }

    @PostMapping("/api/opportunities")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public SalesOpportunity create(
            @Valid @RequestBody OpportunityRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String bearer) {
        requireOpportunityCustomer(request.customerId());
        requireEmployee(request.employeeId(), bearer);
        var row = new SalesOpportunity();
        apply(row, request);
        SalesOpportunity saved = opportunities.save(row);
        recordStageChange(saved.getId(), null, saved.getStage());
        return saved;
    }

    @PutMapping("/api/opportunities/{id}")
    @Transactional
    public SalesOpportunity update(
            @PathVariable Long id,
            @Valid @RequestBody OpportunityRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String bearer) {
        requireOpportunityCustomer(request.customerId());
        requireEmployee(request.employeeId(), bearer);
        var row = findOpportunity(id);
        String oldStage = row.getStage();
        apply(row, request);
        SalesOpportunity saved = opportunities.save(row);
        if (!oldStage.equals(saved.getStage())) {
            recordStageChange(id, oldStage, saved.getStage());
        }
        return saved;
    }

    @PatchMapping("/api/opportunities/{id}/stage")
    @Transactional
    public SalesOpportunity changeStage(
            @PathVariable Long id,
            @Valid @RequestBody OpportunityStageRequest request) {
        SalesOpportunity row = findOpportunity(id);
        String oldStage = row.getStage();
        row.setStage(request.stage());
        SalesOpportunity saved = opportunities.save(row);
        if (!oldStage.equals(saved.getStage())) {
            recordStageChange(id, oldStage, saved.getStage());
        }
        return saved;
    }

    @DeleteMapping("/api/opportunities/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id) {
        SalesOpportunity row = findOpportunity(id);
        if ("WON".equals(row.getStage())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Won opportunities cannot be deleted");
        }
        stageHistory.deleteByOpportunityId(id);
        opportunities.delete(row);
    }

    private void apply(SalesOpportunity row, OpportunityRequest request) {
        row.setCustomerId(request.customerId());
        row.setEmployeeId(request.employeeId());
        row.setTitle(request.title().trim());
        row.setDescription(request.description());
        row.setEstimatedAmount(request.estimatedAmount());
        row.setStage(request.stage() == null ? "NEW" : request.stage());
        row.setExpectedCloseDate(request.expectedCloseDate());
    }

    private void recordStageChange(Long id, String previous, String next) {
        var entry = new OpportunityStageHistory();
        entry.setOpportunityId(id);
        entry.setPreviousStage(previous);
        entry.setNewStage(next);
        stageHistory.save(entry);
    }

    private SalesOpportunity findOpportunity(Long id) {
        return opportunities.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Opportunity not found"));
    }

    private void requireOpportunityCustomer(Long id) {
        var customer = customers.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
        if ("SUPPLIER".equals(customer.getContactType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A supplier cannot own a sales opportunity");
        }
    }

    private void requireCustomer(Long id) {
        if (!customers.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found");
        }
    }

    private void requireEmployee(Long id, String bearer) {
        if (id == null) return;
        if (bearer == null || !bearer.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        employeeDirectory.requireExisting(id, bearer);
    }
}
