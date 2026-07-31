package com.crm.invoice.service.impl;

import com.crm.invoice.dto.InvoiceRequest;
import com.crm.invoice.dto.InvoiceResponse;
import com.crm.invoice.entity.Invoice;
import com.crm.invoice.exception.ResourceNotFoundException;
import com.crm.invoice.mapper.InvoiceMapper;
import com.crm.invoice.repository.InvoiceRepository;
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

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository,
                              InvoiceMapper invoiceMapper) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceMapper = invoiceMapper;
    }

    @Override
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        if (invoiceRepository.existsByInvoiceNumber(request.getInvoiceNumber())) {
            throw new IllegalArgumentException("Invoice number already exists: " + request.getInvoiceNumber());
        }

        Invoice invoice = invoiceMapper.toEntity(request);
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
        Invoice existing = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));

        if (!existing.getInvoiceNumber().equals(request.getInvoiceNumber())
                && invoiceRepository.existsByInvoiceNumber(request.getInvoiceNumber())) {
            throw new IllegalArgumentException("Invoice number already exists: " + request.getInvoiceNumber());
        }

        invoiceMapper.updateEntityFromRequest(existing, request);
        calculateTotalAmount(existing);
        Invoice saved = invoiceRepository.save(existing);
        return invoiceMapper.toResponse(saved);
    }

    @Override
    public void deleteInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
        invoiceRepository.delete(invoice);
    }

    /**
     * Calculate totalAmount = subtotal + tax
     * Handles null values safely.
     */
    private void calculateTotalAmount(Invoice invoice) {
        BigDecimal sub = invoice.getSubtotal() != null ? invoice.getSubtotal() : BigDecimal.ZERO;
        BigDecimal tax = invoice.getTax() != null ? invoice.getTax() : BigDecimal.ZERO;
        invoice.setTotalAmount(sub.add(tax));
    }
}
