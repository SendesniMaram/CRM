package com.crm.invoice.controller;

import com.crm.invoice.dto.InvoiceRequest;
import com.crm.invoice.dto.InvoiceResponse;
import com.crm.invoice.service.IInvoiceService;
import com.crm.invoice.entity.InvoicePayment;
import com.crm.invoice.entity.InvoiceReminder;
import com.crm.invoice.repository.InvoicePaymentRepository;
import com.crm.invoice.repository.InvoiceReminderRepository;
import com.crm.invoice.repository.InvoiceRepository;
import com.crm.invoice.dto.InvoicePaymentRequest;
import com.crm.invoice.dto.InvoiceReminderRequest;
import com.crm.invoice.exception.ResourceNotFoundException;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.math.BigDecimal;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping(path = "/api/invoices", produces = MediaType.APPLICATION_JSON_VALUE)
public class InvoiceController {

    private final IInvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;
    private final InvoicePaymentRepository paymentRepository;
    private final InvoiceReminderRepository reminderRepository;

    public InvoiceController(IInvoiceService invoiceService, InvoiceRepository invoiceRepository,
                             InvoicePaymentRepository paymentRepository, InvoiceReminderRepository reminderRepository) {
        this.invoiceService = invoiceService;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.reminderRepository = reminderRepository;
    }

    public record PaymentSummary(List<InvoicePayment> payments, BigDecimal paidAmount, BigDecimal outstandingAmount) { }

    @GetMapping("/{id}/payments")
    public ResponseEntity<PaymentSummary> payments(@PathVariable Long id) {
        var invoice = invoiceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
        var rows = paymentRepository.findByInvoiceIdOrderByPaidAtDescIdDesc(id);
        var paid = paymentRepository.sumForInvoice(id);
        return ResponseEntity.ok(new PaymentSummary(rows, paid, invoice.getTotalAmount().subtract(paid)));
    }

    @PostMapping("/{id}/payments")
    @Transactional
    public ResponseEntity<InvoicePayment> recordPayment(@PathVariable Long id, @Valid @RequestBody InvoicePaymentRequest request) {
        var invoice = invoiceRepository.lockById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
        var paid = paymentRepository.sumForInvoice(id);
        var outstanding = invoice.getTotalAmount().subtract(paid);
        if (outstanding.signum() <= 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice is already fully paid");
        if (request.amount().compareTo(outstanding) > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment exceeds outstanding amount");
        var payment = new InvoicePayment(); payment.setInvoiceId(id); payment.setAmount(request.amount());
        payment.setPaidAt(request.paidAt());
        payment.setMethod(request.method()); payment.setReference(request.reference()); payment.setNotes(request.notes());
        var saved = paymentRepository.save(payment);
        invoice.setStatus(request.amount().compareTo(outstanding) == 0 ? "PAID" : "PARTIALLY_PAID");
        invoiceRepository.save(invoice);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}/reminders")
    public ResponseEntity<List<InvoiceReminder>> reminders(@PathVariable Long id) {
        if (!invoiceRepository.existsById(id)) throw new ResourceNotFoundException("Invoice not found with id: " + id);
        return ResponseEntity.ok(reminderRepository.findByInvoiceIdOrderByRecordedAtDesc(id));
    }

    @PostMapping("/{id}/reminders")
    @Transactional
    public ResponseEntity<InvoiceReminder> recordReminder(@PathVariable Long id, @Valid @RequestBody InvoiceReminderRequest request) {
        if (!invoiceRepository.existsById(id)) throw new ResourceNotFoundException("Invoice not found with id: " + id);
        var reminder = new InvoiceReminder(); reminder.setInvoiceId(id); reminder.setChannel(request.channel()); reminder.setNotes(request.notes());
        var auth = SecurityContextHolder.getContext().getAuthentication();
        reminder.setRecordedBy(auth == null ? null : auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(reminderRepository.save(reminder));
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody InvoiceRequest request) {
        InvoiceResponse response = invoiceService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String keyword) {

        if (page == 0 && size == 10 && sortBy == null && direction.equals("asc") && keyword == null) {
            List<InvoiceResponse> invoices = invoiceService.getAllInvoices();
            return ResponseEntity.ok(invoices);
        }

        Page<InvoiceResponse> invoicePage = invoiceService.getAllInvoicesPaged(page, size, sortBy, direction, keyword);
        return ResponseEntity.ok(invoicePage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable Long id) {
        InvoiceResponse response = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvoiceResponse> updateInvoice(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceRequest request) {
        InvoiceResponse response = invoiceService.updateInvoice(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }
}
