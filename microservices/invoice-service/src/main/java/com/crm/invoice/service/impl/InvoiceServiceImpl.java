package com.crm.invoice.service.impl;

import com.crm.invoice.dto.InvoiceRequest;
import com.crm.invoice.dto.InvoiceResponse;
import com.crm.invoice.entity.Invoice;
import com.crm.invoice.exception.ResourceNotFoundException;
import com.crm.invoice.mapper.InvoiceMapper;
import com.crm.invoice.repository.InvoiceRepository;
import com.crm.invoice.repository.InvoiceNumberSequenceRepository;
import com.crm.invoice.service.IInvoiceService;
import com.crm.invoice.util.InvoiceSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class InvoiceServiceImpl implements IInvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceNumberSequenceRepository sequenceRepository;
    private final com.crm.invoice.repository.InvoicePaymentRepository payments;
    private final com.crm.invoice.repository.InvoiceReminderRepository reminders;
    @org.springframework.beans.factory.annotation.Autowired(required=false) private com.crm.invoice.repository.ReminderTaskRepository tasks;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository,
                              InvoiceMapper invoiceMapper, InvoiceNumberSequenceRepository sequenceRepository,
                              com.crm.invoice.repository.InvoicePaymentRepository payments,
                              com.crm.invoice.repository.InvoiceReminderRepository reminders) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceMapper = invoiceMapper;
        this.sequenceRepository = sequenceRepository;
        this.payments = payments;
        this.reminders = reminders;
    }

    @Override
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        com.crm.invoice.service.FinancialValidation.request(request);
        requireUnpaidStatus(request.getStatus());
        Invoice invoice = invoiceMapper.toEntity(request);
        invoice.setInvoiceNumber(nextInvoiceNumber());
        calculateTotalAmount(invoice);
        Invoice saved = invoiceRepository.save(invoice);
        return invoiceMapper.toResponse(saved);
    }

    @Override
    public InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    public List<InvoiceResponse> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }

    @Override
    public Page<InvoiceResponse> getAllInvoicesPaged(int page, int size, String sortBy, String direction, String keyword) {
        String sortField = StringUtils.hasText(sortBy) ? sortBy : "id";
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(sortDirection, sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Invoice> invoicePage;

        if (StringUtils.hasText(keyword)) {
            invoicePage = invoiceRepository.findAll(
                    InvoiceSpecification.searchByKeyword(keyword), pageable);
        } else {
            invoicePage = invoiceRepository.findAll(pageable);
        }

        return invoicePage.map(invoiceMapper::toResponse);
    }

    @Override
    public InvoiceResponse updateInvoice(Long id, InvoiceRequest request) {
        com.crm.invoice.service.FinancialValidation.request(request);
        Invoice existing = invoiceRepository.lockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));

        requireEditable(existing);
        requireUnpaidStatus(request.getStatus());
        // Quote lines remain authoritative: this DTO cannot edit those lines.
        if (!existing.getLines().isEmpty() && (existing.getSubtotal().compareTo(request.getSubtotal()) != 0
                || !java.util.Objects.equals(existing.getCustomerId(), request.getCustomerId())
                || !java.util.Objects.equals(existing.getCurrency(), request.getCurrency())))
            throw conflict("Quote invoice lines, customer and currency cannot be changed directly");
        invoiceMapper.updateEntityFromRequest(existing, request);
        calculateTotalAmount(existing);
        Invoice saved = invoiceRepository.save(existing);
        return invoiceMapper.toResponse(saved);
    }

    @Override
    public void deleteInvoice(Long id) {
        Invoice invoice = invoiceRepository.lockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
        com.crm.invoice.service.InvoiceLifecycle.archive(invoice,payments.sumForInvoice(id));
        if(tasks!=null)tasks.cancelForInvoice(id);

    }

    /**
     * Calculate totalAmount = subtotal + tax
     * Handles null values safely.
     */
    private void calculateTotalAmount(Invoice invoice) {
        BigDecimal sub = invoice.getSubtotal() != null ? invoice.getSubtotal() : BigDecimal.ZERO;
        BigDecimal tax = invoice.getTax() != null ? invoice.getTax() : BigDecimal.ZERO;
        com.crm.invoice.service.FinancialValidation.calculated(sub.add(tax));
        invoice.setTotalAmount(sub.add(tax));
    }

    private void requireEditable(Invoice invoice) {
        if(invoice.isIssued()||invoice.isArchived()||invoice.getReconciliationId()!=null)throw conflict("Issued, archived or source-linked invoice is immutable");
        // Same row lock as recordPayment, including when the first payment is inserted.
        if (payments.existsByInvoiceId(invoice.getId()))
            throw conflict("An invoice with recorded payments cannot be edited or deleted");
        if (!java.util.Set.of("DRAFT", "PENDING", "UNPAID").contains(invoice.getStatus()))
            throw conflict("Invoice state does not allow direct changes");
    }

    private void requireUnpaidStatus(String status) {
        if (!java.util.Set.of("DRAFT", "PENDING", "UNPAID").contains(status))
            throw conflict("Financial status is derived from recorded payments");
    }

    private org.springframework.web.server.ResponseStatusException conflict(String message) {
        return new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, message);
    }

    private String nextInvoiceNumber() {
        var sequence = sequenceRepository.lockSequence();
        if (sequence == null) throw new IllegalStateException("Invoice number sequence is not initialized");
        long value = sequence.getNextValue();
        sequence.setNextValue(value + 1);
        sequenceRepository.save(sequence);
        return "INV-" + java.time.Year.now().getValue() + "-" + String.format("%06d", value);
    }
}
