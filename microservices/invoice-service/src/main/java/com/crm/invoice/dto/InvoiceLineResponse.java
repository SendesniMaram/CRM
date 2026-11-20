package com.crm.invoice.dto;
import java.math.BigDecimal;
public record InvoiceLineResponse(Long id, String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal lineTotal) { }
