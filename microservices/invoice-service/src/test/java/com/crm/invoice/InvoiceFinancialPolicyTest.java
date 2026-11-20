package com.crm.invoice;

import com.crm.invoice.controller.InvoiceController;
import com.crm.invoice.dto.InvoicePaymentRequest;
import com.crm.invoice.dto.InvoiceRequest;
import com.crm.invoice.entity.Invoice;
import com.crm.invoice.entity.InvoiceLine;
import com.crm.invoice.mapper.InvoiceMapper;
import com.crm.invoice.repository.*;
import com.crm.invoice.service.impl.InvoiceServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Plain unit tests: no Spring application, database or migration. */
class InvoiceFinancialPolicyTest {
    InvoiceRepository invoices;
    InvoicePaymentRepository payments;
    InvoiceReminderRepository reminders;
    InvoiceServiceImpl service;
    InvoiceController controller;
    Invoice row;
    InvoiceRequest request;

    @BeforeEach void setup() {
        invoices = mock(InvoiceRepository.class);
        payments = mock(InvoicePaymentRepository.class);
        reminders = mock(InvoiceReminderRepository.class);
        service = new InvoiceServiceImpl(invoices, new InvoiceMapper(), mock(InvoiceNumberSequenceRepository.class), payments, reminders);
        controller = new InvoiceController(service, invoices, payments, reminders);
        row = new Invoice(); row.setId(1L); row.setInvoiceNumber("INV-UNCHANGED");
        row.setCustomerId("2"); row.setSubtotal(new BigDecimal("100.00"));
        row.setTax(BigDecimal.ZERO); row.setTotalAmount(new BigDecimal("100.00")); row.setStatus("PENDING");
        when(invoices.lockById(1L)).thenReturn(Optional.of(row));
        when(invoices.save(any())).thenAnswer(call -> call.getArgument(0));
        when(payments.sumForInvoice(1L)).thenReturn(BigDecimal.ZERO);
        request = new InvoiceRequest(); request.setCustomerId("2"); request.setSubtotal(new BigDecimal("100.00"));
        request.setStatus("PENDING");
    }

    @Test void editsUnpaidInvoiceAndKeepsGeneratedNumber() {
        request.setSubtotal(new BigDecimal("90.00"));
        assertEquals(0, service.updateInvoice(1L, request).getTotalAmount().compareTo(new BigDecimal("90.00")));
        assertEquals("INV-UNCHANGED", row.getInvoiceNumber());
        var order = inOrder(invoices, payments);
        order.verify(invoices).lockById(1L);
        order.verify(payments).existsByInvoiceId(1L);
        order.verify(invoices).save(row);
    }

    @ParameterizedTest @ValueSource(strings = {"PARTIALLY_PAID", "PAID", "PENDING"})
    void recordedPaymentsPreventAllEditsAndDeletionEvenIfHistoricalStatusIsWrong(String status) {
        row.setStatus(status); when(payments.existsByInvoiceId(1L)).thenReturn(true);
        request.setSubtotal(new BigDecimal("50"));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.updateInvoice(1L, request)).getStatusCode().value());
        assertThrows(ResponseStatusException.class, () -> service.deleteInvoice(1L));
        assertEquals(new BigDecimal("100.00"), row.getSubtotal());
        verify(invoices, never()).save(any()); verify(invoices, never()).delete(any(Invoice.class));
    }

    @Test void rejectsLegacyPaidStateWithoutPaymentAndForgedFinancialStatus() {
        request.setStatus("PAID");
        assertThrows(ResponseStatusException.class, () -> service.createInvoice(request));
        assertThrows(ResponseStatusException.class, () -> service.updateInvoice(1L, request));
        row.setStatus("PAID");
        assertThrows(ResponseStatusException.class, () -> service.deleteInvoice(1L));
    }

    @Test void preservesLinesAndReminderHistory() {
        row.getLines().add(new InvoiceLine()); request.setSubtotal(new BigDecimal("99"));
        assertThrows(ResponseStatusException.class, () -> service.updateInvoice(1L, request));
        assertThrows(ResponseStatusException.class, () -> service.deleteInvoice(1L));
        row.getLines().clear();
        when(reminders.findByInvoiceIdOrderByRecordedAtDesc(1L)).thenReturn(java.util.List.of(new com.crm.invoice.entity.InvoiceReminder()));
        assertThrows(ResponseStatusException.class, () -> service.deleteInvoice(1L));
    }

    @Test void unpaidInvoiceCannotBePhysicallyDeleted() {
        assertThrows(ResponseStatusException.class,()->service.deleteInvoice(1L));verify(invoices,never()).delete(any(Invoice.class));
    }

    @Test void partialAndCompletePaymentsDeriveStatusAndOutstandingAndRetainRows() {
        when(payments.save(any())).thenAnswer(call -> call.getArgument(0));
        controller.recordPayment(1L, new InvoicePaymentRequest(new BigDecimal("30"), LocalDate.now(), null, null, null));
        assertEquals("PARTIALLY_PAID", row.getStatus());
        when(payments.sumForInvoice(1L)).thenReturn(new BigDecimal("30"));
        when(invoices.findById(1L)).thenReturn(Optional.of(row));
        assertEquals(0, controller.payments(1L).getBody().outstandingAmount().compareTo(new BigDecimal("70")));
        controller.recordPayment(1L, new InvoicePaymentRequest(new BigDecimal("70"), LocalDate.now(), null, null, null));
        assertEquals("PAID", row.getStatus());
        verify(payments, times(2)).save(any()); verify(payments, never()).delete(any());
        assertThrows(ResponseStatusException.class, () -> controller.recordPayment(1L,
                new InvoicePaymentRequest(BigDecimal.ONE, LocalDate.now(), null, null, null)));
    }

    @Test void editThenPaymentUsesNewTotalAndPaymentThenEditIsRejected() {
        request.setSubtotal(new BigDecimal("60")); service.updateInvoice(1L, request);
        assertThrows(ResponseStatusException.class, () -> controller.recordPayment(1L,
                new InvoicePaymentRequest(new BigDecimal("70"), LocalDate.now(), null, null, null)));
        when(payments.existsByInvoiceId(1L)).thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> service.updateInvoice(1L, request));
    }

    @Test void bothWritePathsRequireTransactionAndTheSamePessimisticRowLock() throws Exception {
        assertNotNull(InvoiceServiceImpl.class.getAnnotation(org.springframework.transaction.annotation.Transactional.class));
        assertNotNull(InvoiceController.class.getMethod("recordPayment", Long.class, InvoicePaymentRequest.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class));
        assertEquals(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE,
                InvoiceRepository.class.getMethod("lockById", Long.class).getAnnotation(org.springframework.data.jpa.repository.Lock.class).value());
        controller.recordPayment(1L, new InvoicePaymentRequest(BigDecimal.ONE, LocalDate.now(), null, null, null));
        var order = inOrder(invoices, payments);
        order.verify(invoices).lockById(1L); order.verify(payments).sumForInvoice(1L);
        // Actual MySQL lock waiting/deadlock behavior requires a later integration test.
    }

    @Test void validatesServiceCallsIncludingDatesPrecisionNegativeTaxAndOverflow() {
        request.setTax(new BigDecimal("-1")); assertThrows(IllegalArgumentException.class, () -> service.updateInvoice(1L, request));
        request.setTax(new BigDecimal("0.001")); assertThrows(IllegalArgumentException.class, () -> service.updateInvoice(1L, request));
        request.setTax(BigDecimal.ZERO); request.setIssueDate(LocalDate.of(2026, 2, 2)); request.setDueDate(LocalDate.of(2026, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> service.updateInvoice(1L, request));
        request.setDueDate(null); request.setStatus("unknown"); assertThrows(IllegalArgumentException.class, () -> service.updateInvoice(1L, request));
        request.setStatus("PENDING"); request.setSubtotal(new BigDecimal("9999999999.99")); request.setTax(BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class, () -> service.updateInvoice(1L, request));
    }

    @Test void invalidHttpInputsAreRejectedBeforeCallingTheService() throws Exception {
        var delegate = mock(com.crm.invoice.service.IInvoiceService.class);
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                new InvoiceController(delegate, invoices, payments, reminders))
                .setControllerAdvice(new com.crm.invoice.exception.GlobalExceptionHandler()).build();
        String valid = "{\"customerId\":\"2\",\"subtotal\":100,\"status\":\"PENDING\"}";
        for (String body : new String[]{valid.replace("100", "-1"), valid.replace("100", "1.001"),
                valid.replace("PENDING", "UNKNOWN"), valid.replace("100", "10000000000"),
                valid.replace("}", ",\"tax\":-1}"), valid.replace("}", ",\"issueDate\":\"2026-11-02\",\"dueDate\":\"2026-11-01\"}"),
                "{}"}) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/invoices")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        }
        verifyNoInteractions(delegate);
    }

    @Test void recordingAReminderAlsoLocksTheInvoiceBeforeWritingHistory() {
        row.setDueDate(LocalDate.now());
        controller.recordReminder(1L, new com.crm.invoice.dto.InvoiceReminderRequest("MANUAL", "Follow-up"));
        var order = inOrder(invoices, reminders);
        order.verify(invoices).lockById(1L); order.verify(reminders).save(any());
    }
}
