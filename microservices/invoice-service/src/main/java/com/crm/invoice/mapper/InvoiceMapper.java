package com.crm.invoice.mapper;

import com.crm.invoice.dto.InvoiceRequest;
import com.crm.invoice.dto.InvoiceResponse;
import com.crm.invoice.entity.Invoice;
import org.springframework.stereotype.Component;

@Component
public class InvoiceMapper {

    public InvoiceResponse toResponse(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        InvoiceResponse response = new InvoiceResponse();
        response.setId(invoice.getId());
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setCustomerId(invoice.getCustomerId());
        response.setIssueDate(invoice.getIssueDate());
        response.setDueDate(invoice.getDueDate());
        response.setSubtotal(invoice.getSubtotal());
        response.setTax(invoice.getTax());
        response.setTotalAmount(invoice.getTotalAmount());
        response.setCurrency(invoice.getCurrency());
        response.setStatus(invoice.getStatus());
        response.setNotes(invoice.getNotes());
        response.setCreatedAt(invoice.getCreatedAt());
        response.setUpdatedAt(invoice.getUpdatedAt());

        return response;
    }

    public Invoice toEntity(InvoiceRequest request) {
        if (request == null) {
            return null;
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(request.getInvoiceNumber());
        invoice.setCustomerId(request.getCustomerId());
        invoice.setIssueDate(request.getIssueDate());
        invoice.setDueDate(request.getDueDate());
        invoice.setSubtotal(request.getSubtotal());
        invoice.setTax(request.getTax());
        invoice.setCurrency(request.getCurrency());
        invoice.setStatus(request.getStatus());
        invoice.setNotes(request.getNotes());

        return invoice;
    }

    public void updateEntityFromRequest(Invoice invoice, InvoiceRequest request) {
        if (request == null) {
            return;
        }
        invoice.setInvoiceNumber(request.getInvoiceNumber());
        invoice.setCustomerId(request.getCustomerId());
        invoice.setIssueDate(request.getIssueDate());
        invoice.setDueDate(request.getDueDate());
        invoice.setSubtotal(request.getSubtotal());
        invoice.setTax(request.getTax());
        invoice.setCurrency(request.getCurrency());
        invoice.setStatus(request.getStatus());
        invoice.setNotes(request.getNotes());
    }
}
