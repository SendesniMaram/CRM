package com.crm.invoice.service;

import com.crm.invoice.dto.InvoiceRequest;
import com.crm.invoice.dto.InvoiceResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IInvoiceService {

    InvoiceResponse createInvoice(InvoiceRequest request);

    InvoiceResponse getInvoiceById(Long id);

    List<InvoiceResponse> getAllInvoices();

    Page<InvoiceResponse> getAllInvoicesPaged(int page, int size, String sortBy, String direction, String keyword);

    InvoiceResponse updateInvoice(Long id, InvoiceRequest request);

    void deleteInvoice(Long id);
}
