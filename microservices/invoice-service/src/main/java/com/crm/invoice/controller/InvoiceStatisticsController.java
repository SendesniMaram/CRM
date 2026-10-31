package com.crm.invoice.controller;

import com.crm.invoice.repository.InvoicePaymentRepository;
import com.crm.invoice.repository.InvoiceRepository;
import com.crm.invoice.repository.QuoteRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceStatisticsController {
    private final InvoiceRepository invoices;
    private final InvoicePaymentRepository payments;
    private final QuoteRepository quotes;

    public InvoiceStatisticsController(InvoiceRepository invoices, InvoicePaymentRepository payments, QuoteRepository quotes) {
        this.invoices = invoices;
        this.payments = payments;
        this.quotes = quotes;
    }

    public record MoneySummary(long count, BigDecimal total, BigDecimal paid, BigDecimal outstanding) { }
    public record Summary(Map<String, Long> invoiceCountsByStatus, Map<String, MoneySummary> invoicesByCurrency,
                          Map<String, MoneySummary> quotesByCurrency, Map<String, Long> quoteCountsByStatus) { }

    @GetMapping("/statistics")
    public Summary statistics() {
        Map<String, Long> invoiceCounts = new TreeMap<>();
        Map<String, BigDecimal> invoiceTotals = new TreeMap<>();
        var invoiceRows = invoices.summarizeByStatusAndCurrency();
        for (var row : invoiceRows) {
            invoiceCounts.merge(row.getStatus(), row.getCount(), Long::sum);
            invoiceTotals.merge(currency(row.getCurrency()), zero(row.getTotal()), BigDecimal::add);
        }
        Map<String, BigDecimal> paidTotals = new TreeMap<>();
        for (var row : payments.summarizePaidByCurrency())
            paidTotals.put(currency(row.getCurrency()), zero(row.getAmount()));
        Map<String, MoneySummary> invoiceMoney = new TreeMap<>();
        invoiceTotals.forEach((currency, total) -> {
            BigDecimal paid = paidTotals.getOrDefault(currency, BigDecimal.ZERO);
            long count = invoiceRows.stream()
                    .filter(row -> currency(row.getCurrency()).equals(currency)).mapToLong(InvoiceRepository.StatusCurrencySummary::getCount).sum();
            invoiceMoney.put(currency, new MoneySummary(count, total, paid, total.subtract(paid).max(BigDecimal.ZERO)));
        });
        Map<String, Long> quoteCounts = new TreeMap<>();
        Map<String, BigDecimal> quoteTotals = new TreeMap<>();
        var quoteRows = quotes.summarizeByStatusAndCurrency();
        for (var row : quoteRows) {
            quoteCounts.merge(row.getStatus(), row.getCount(), Long::sum);
            quoteTotals.merge(currency(row.getCurrency()), zero(row.getTotal()), BigDecimal::add);
        }
        Map<String, MoneySummary> quoteMoney = new TreeMap<>();
        quoteTotals.forEach((currency, total) -> quoteMoney.put(currency, new MoneySummary(
                quoteRows.stream().filter(row -> currency(row.getCurrency()).equals(currency))
                        .mapToLong(QuoteRepository.StatusCurrencySummary::getCount).sum(), total, BigDecimal.ZERO, total)));
        return new Summary(invoiceCounts, invoiceMoney, quoteMoney, quoteCounts);
    }

    private String currency(String value) { return value == null || value.isBlank() ? "NON_RENSEIGNÉE" : value.trim(); }
    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
