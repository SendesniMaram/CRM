package com.crm.customer.controller;

import com.crm.customer.repository.CustomerRepository;
import com.crm.customer.repository.SalesOpportunityRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerDashboardController {
    private final CustomerRepository customers;
    private final SalesOpportunityRepository opportunities;

    public CustomerDashboardController(CustomerRepository customers, SalesOpportunityRepository opportunities) {
        this.customers = customers;
        this.opportunities = opportunities;
    }

    public record OpportunityStage(String stage, long count, BigDecimal estimatedAmount) { }
    public record Summary(long customers, long prospects, long suppliers, Map<String, OpportunityStage> opportunitiesByStage) { }

    @GetMapping("/statistics")
    public Summary statistics() {
        Map<String, OpportunityStage> byStage = new TreeMap<>();
        opportunities.summarizeStages().forEach(row -> byStage.put(row.getStage(),
                new OpportunityStage(row.getStage(), row.getCount(), row.getAmount() == null ? BigDecimal.ZERO : row.getAmount())));
        return new Summary(customers.countByContactType("CUSTOMER"), customers.countByContactType("LEAD"),
                customers.countByContactType("SUPPLIER"), byStage);
    }
}
