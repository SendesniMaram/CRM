package com.crm.fees.controller;

import com.crm.fees.repository.FeeRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fees")
public class FeeStatisticsController {
    private final FeeRepository fees;
    public FeeStatisticsController(FeeRepository fees) { this.fees = fees; }
    public interface Totals { long getCount(); BigDecimal getAmount(); Double getHours(); }
    public interface StatusCount { String getStatus(); long getCount(); }
    public record Summary(long count, BigDecimal amount, Double workedHours,
                          Map<String, Long> invoiceStatuses, Map<String, Long> paymentStatuses,
                          boolean currencyDefined) { }

    @GetMapping("/statistics")
    public Summary statistics() {
        var totals = fees.aggregateTotals();
        Map<String, Long> invoice = new TreeMap<>(), payment = new TreeMap<>();
        fees.countByInvoiceStatus().forEach(row -> invoice.put(label(row.getStatus()), row.getCount()));
        fees.countByPaymentStatus().forEach(row -> payment.put(label(row.getStatus()), row.getCount()));
        return new Summary(totals.getCount(), null,
                totals.getHours() == null ? 0d : totals.getHours(), invoice, payment, false);
    }
    private String label(String value) { return value == null || value.isBlank() ? "NON_RENSEIGNÉ" : value; }
}
