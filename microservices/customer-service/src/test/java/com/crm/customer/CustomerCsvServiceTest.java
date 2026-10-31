package com.crm.customer;

import com.crm.customer.dto.CustomerRequest;
import com.crm.customer.dto.CustomerResponse;
import com.crm.customer.service.CustomerCsvService;
import com.crm.customer.service.ICustomerService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipInputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CustomerCsvServiceTest {
    private static final ValidatorContext VALIDATOR = new ValidatorContext();
    private final ICustomerService customers = mock(ICustomerService.class);
    private final CustomerCsvService service = new CustomerCsvService(customers, VALIDATOR.validator);

    @BeforeEach
    void reset() {
        Mockito.reset(customers);
    }

    @AfterAll
    static void closeValidator() {
        VALIDATOR.factory.close();
    }

    @Test
    void importsValidRowsAndReportsInvalidAndDuplicateRowsWithoutReplacingContacts() {
        when(customers.createCustomer(any(CustomerRequest.class))).thenAnswer(invocation -> {
            CustomerRequest request = invocation.getArgument(0);
            if ("Existing".equals(request.getName())) throw new IllegalArgumentException("Customer name already exists");
            return new CustomerResponse();
        });

        var report = service.importCsv("""
                name,email,contactType,category
                "Acme, Inc.",sales@example.test,LEAD,Technology
                ,invalid,UNKNOWN,
                Existing,existing@example.test,CUSTOMER,
                """.getBytes(StandardCharsets.UTF_8));

        assertEquals(3, report.totalRows());
        assertEquals(1, report.acceptedRows());
        assertEquals(2, report.rejectedRows());
        assertEquals(List.of(3, 4), report.issues().stream().map(CustomerCsvService.ImportIssue::row).toList());
        verify(customers, times(2)).createCustomer(any(CustomerRequest.class));
        verify(customers, never()).updateCustomer(anyLong(), any());
    }

    @Test
    void rejectsUnknownOrDuplicatedHeadersAndMalformedCsv() {
        assertThrows(IllegalArgumentException.class, () -> service.importCsv("name,unexpected\nAcme,x".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class, () -> service.importCsv("name,name\nAcme,Other".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class, () -> service.importCsv("name,email\n\"unterminated,x".getBytes(StandardCharsets.UTF_8)));
        verifyNoInteractions(customers);
    }

    @Test
    void exportsFilteredContactsAsQuotedUtf8CsvAndPreventsSpreadsheetFormulaExecution() {
        CustomerResponse row = new CustomerResponse();
        row.setId(7L); row.setName("=HYPERLINK(\"https://example.test\")");
        row.setEmail("crm@example.test"); row.setCompany("Acme, Europe");
        when(customers.getAllCustomersPaged(anyInt(), anyInt(), any(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 100), 1));

        String csv = new String(service.exportCsv("Acme", "LEAD", "Tech", "SMB", 9L), StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("\uFEFF\"name\",\"email\""));
        assertTrue(csv.contains("\"'=HYPERLINK(\"\"https://example.test\"\")\""));
        assertTrue(csv.contains("\"Acme, Europe\""));
        verify(customers).getAllCustomersPaged(0, 100, "id", "asc", "Acme", "LEAD", "Tech", "SMB", 9L);
    }

    @Test
    void exportsAnExcelWorkbookWithContactCellsStoredAsText() throws Exception {
        CustomerResponse row = new CustomerResponse();
        row.setId(8L); row.setName("=SUM(A1:A2)"); row.setAssignedEmployeeId(31L);
        when(customers.getAllCustomersPaged(anyInt(), anyInt(), any(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 100), 1));

        byte[] workbook = service.exportXlsx(null, null, null, null, null);
        java.util.Map<String, String> entries = new java.util.HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(workbook), StandardCharsets.UTF_8)) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry())
                entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertTrue(entries.containsKey("xl/workbook.xml"));
        assertTrue(entries.get("xl/worksheets/sheet1.xml").contains("t=\"inlineStr\""));
        assertTrue(entries.get("xl/worksheets/sheet1.xml").contains("&apos;=SUM(A1:A2)"));
        assertTrue(entries.get("xl/worksheets/sheet1.xml").contains("assignedEmployeeId"));
    }

    private static final class ValidatorContext {
        final jakarta.validation.ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        final jakarta.validation.Validator validator = factory.getValidator();
    }
}
