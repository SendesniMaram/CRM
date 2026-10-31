package com.crm.invoice.controller;

import com.crm.invoice.dto.InvoiceLineResponse;
import com.crm.invoice.dto.InvoiceRequest;
import com.crm.invoice.dto.InvoiceResponse;
import com.crm.invoice.dto.QuoteRequest;
import com.crm.invoice.entity.Invoice;
import com.crm.invoice.entity.InvoiceLine;
import com.crm.invoice.entity.Quote;
import com.crm.invoice.entity.QuoteLine;
import com.crm.invoice.pdf.PdfReport;
import com.crm.invoice.repository.InvoiceRepository;
import com.crm.invoice.repository.QuoteRepository;
import com.crm.invoice.service.IInvoiceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Validated
@RequestMapping(path = "/api/quotes", produces = MediaType.APPLICATION_JSON_VALUE)
public class QuoteController {
    private static final Set<String> STATUSES = Set.of("DRAFT", "SENT", "ACCEPTED", "REJECTED");
    private static final Set<String> SORT_FIELDS = Set.of("id", "quoteNumber", "customerId", "issueDate",
            "expiryDate", "subtotal", "totalAmount", "status", "createdAt");

    private final QuoteRepository quotes;
    private final IInvoiceService invoices;
    private final InvoiceRepository invoiceRepository;

    public QuoteController(QuoteRepository quotes, IInvoiceService invoices, InvoiceRepository invoiceRepository) {
        this.quotes = quotes;
        this.invoices = invoices;
        this.invoiceRepository = invoiceRepository;
    }

    public record StatusRequest(@NotBlank @Pattern(regexp = "DRAFT|SENT|ACCEPTED|REJECTED") String status) { }
    public record ConversionResponse(Quote quote, InvoiceResponse invoice) { }

    @PostMapping
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public Quote create(@Valid @RequestBody QuoteRequest request) {
        requireDraft(request.status());
        Quote quote = from(request);
        totals(quote);
        return quotes.save(quote);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Page<Quote> all(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        if (page < 0 || size < 1 || size > 100 || !SORT_FIELDS.contains(sortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid quote page options");
        }
        if (status != null && !STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported quote status");
        }
        var sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        var pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<Quote> pageResult = quotes.findAll(filters(keyword, status, customerId), pageable);
        if (pageResult.isEmpty()) return pageResult;
        List<Long> ids = pageResult.getContent().stream().map(Quote::getId).toList();
        Map<Long, Quote> withLines = quotes.findWithLinesByIdIn(ids).stream()
                .collect(Collectors.toMap(Quote::getId, Function.identity()));
        List<Quote> content = ids.stream().map(withLines::get).toList();
        return new PageImpl<>(content, pageable, pageResult.getTotalElements());
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public Quote get(@PathVariable Long id) {
        return quotes.findWithLinesById(id).orElseThrow(() -> notFound(id));
    }

    @PutMapping("/{id}")
    @Transactional
    public Quote update(@PathVariable Long id, @Valid @RequestBody QuoteRequest request) {
        requireDraft(request.status());
        Quote quote = quotes.lockWithLines(id).orElseThrow(() -> notFound(id));
        if (quote.getConvertedInvoiceId() != null || !"DRAFT".equals(quote.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft quotes can be edited");
        }
        validateDates(request);
        quote.setCustomerId(request.customerId());
        quote.setIssueDate(request.issueDate());
        quote.setExpiryDate(request.expiryDate());
        quote.setTax(zero(request.tax()));
        quote.setCurrency(request.currency());
        quote.setNotes(request.notes());
        quote.getLines().clear();
        quote.getLines().addAll(lines(request.lines()));
        totals(quote);
        return quotes.save(quote);
    }

    @PatchMapping("/{id}/status")
    @Transactional
    public Quote status(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        Quote quote = quotes.lockWithLines(id).orElseThrow(() -> notFound(id));
        if (quote.getConvertedInvoiceId() != null) return quote;
        if ("REJECTED".equals(quote.getStatus()) || "ACCEPTED".equals(quote.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Final quote status cannot be changed");
        }
        quote.setStatus(request.status());
        return quotes.save(quote);
    }

    @PostMapping("/{id}/convert")
    @Transactional
    public ConversionResponse convert(@PathVariable Long id) {
        Quote quote = quotes.lockWithLines(id).orElseThrow(() -> notFound(id));
        if (quote.getConvertedInvoiceId() != null) {
            return new ConversionResponse(quote, invoices.getInvoiceById(quote.getConvertedInvoiceId()));
        }
        if (!"ACCEPTED".equals(quote.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only accepted quotes can be converted");
        }

        InvoiceRequest request = new InvoiceRequest();
        request.setCustomerId(quote.getCustomerId());
        request.setIssueDate(quote.getIssueDate());
        request.setDueDate(quote.getExpiryDate());
        request.setSubtotal(quote.getSubtotal());
        request.setTax(quote.getTax());
        request.setCurrency(quote.getCurrency());
        request.setStatus("PENDING");
        request.setNotes(quote.getNotes());
        InvoiceResponse invoice = invoices.createInvoice(request);
        Invoice entity = invoiceRepository.findById(invoice.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Created invoice could not be loaded"));
        entity.getLines().clear();
        for (QuoteLine quoteLine : quote.getLines()) {
            InvoiceLine line = new InvoiceLine();
            line.setDescription(quoteLine.getDescription());
            line.setQuantity(quoteLine.getQuantity());
            line.setUnitPrice(quoteLine.getUnitPrice());
            line.setLineTotal(quoteLine.getLineTotal());
            entity.getLines().add(line);
        }
        invoiceRepository.save(entity);
        quote.setConvertedInvoiceId(invoice.getId());
        Quote saved = quotes.save(quote);
        List<InvoiceLineResponse> lines = entity.getLines().stream()
                .map(line -> new InvoiceLineResponse(line.getId(), line.getDescription(),
                        line.getQuantity(), line.getUnitPrice(), line.getLineTotal()))
                .toList();
        invoice.setLines(lines);
        return new ConversionResponse(saved, invoice);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        Quote quote = get(id);
        PdfReport.requireAdmin();
        List<PdfReport.Row> rows = new ArrayList<>();
        rows.add(new PdfReport.Row("Client", quote.getCustomerId()));
        rows.add(new PdfReport.Row("Statut", quote.getStatus()));
        for (QuoteLine line : quote.getLines()) {
            rows.add(new PdfReport.Row(line.getDescription(),
                    line.getQuantity() + " × " + line.getUnitPrice() + " = " + line.getLineTotal()));
        }
        rows.add(new PdfReport.Row("Sous-total", PdfReport.amount(quote.getSubtotal(), quote.getCurrency())));
        rows.add(new PdfReport.Row("Taxe (montant saisi)", PdfReport.amount(quote.getTax(), quote.getCurrency())));
        rows.add(new PdfReport.Row("Total", PdfReport.amount(quote.getTotalAmount(), quote.getCurrency())));
        return PdfReport.attachment(PdfReport.render("Devis", quote.getQuoteNumber(), rows),
                quote.getQuoteNumber() + ".pdf");
    }

    private Specification<Quote> filters(String keyword, String status, String customerId) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("quoteNumber")), pattern),
                        cb.like(cb.lower(root.get("customerId")), pattern)));
            }
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (StringUtils.hasText(customerId)) predicates.add(cb.equal(root.get("customerId"), customerId.trim()));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Quote from(QuoteRequest request) {
        validateDates(request);
        Quote quote = new Quote();
        quote.setQuoteNumber("DEV-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT));
        quote.setCustomerId(request.customerId());
        quote.setIssueDate(request.issueDate());
        quote.setExpiryDate(request.expiryDate());
        quote.setTax(zero(request.tax()));
        quote.setCurrency(request.currency());
        quote.setStatus("DRAFT");
        quote.setNotes(request.notes());
        quote.setLines(lines(request.lines()));
        return quote;
    }

    private List<QuoteLine> lines(List<QuoteRequest.Line> input) {
        return new ArrayList<>(input.stream().map(request -> {
            QuoteLine line = new QuoteLine();
            line.setDescription(request.description().trim());
            line.setQuantity(request.quantity());
            line.setUnitPrice(request.unitPrice());
            line.setLineTotal(request.quantity().multiply(request.unitPrice()).setScale(2, RoundingMode.HALF_UP));
            return line;
        }).toList());
    }

    private void totals(Quote quote) {
        BigDecimal subtotal = quote.getLines().stream().map(QuoteLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.UNNECESSARY);
        if (subtotal.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quote subtotal must be positive");
        }
        quote.setSubtotal(subtotal);
        quote.setTax(zero(quote.getTax()).setScale(2, RoundingMode.UNNECESSARY));
        quote.setTotalAmount(subtotal.add(quote.getTax()).setScale(2, RoundingMode.UNNECESSARY));
    }

    private void validateDates(QuoteRequest request) {
        if (request.issueDate() != null && request.expiryDate() != null
                && request.expiryDate().isBefore(request.issueDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expiry date precedes issue date");
        }
    }

    private void requireDraft(String status) {
        if (status != null && !"DRAFT".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "New and edited quotes must use DRAFT status; use the status endpoint for transitions");
        }
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Quote not found: " + id);
    }
}
