package com.crm.customer.service;

import com.crm.customer.dto.CustomerRequest;
import com.crm.customer.dto.CustomerResponse;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CustomerCsvService {
    private static final int MAX_ROWS = 10_000;
    private static final List<String> HEADERS = List.of(
            "name", "email", "phone", "company", "address", "contactType", "category", "segment", "assignedEmployeeId");
    private static final Set<String> SUPPORTED_HEADERS = Set.of(
            "name", "email", "phone", "company", "address", "contacttype", "category", "segment", "assignedemployeeid");
    private final ICustomerService customers;
    private final Validator validator;

    public CustomerCsvService(ICustomerService customers, Validator validator) {
        this.customers = customers;
        this.validator = validator;
    }

    public ImportReport importCsv(byte[] content) {
        return importCsv(content, customers::createCustomer);
    }

    public ImportReport importCsv(byte[] content, Function<CustomerRequest, CustomerResponse> creator) {
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(content)).toString();
        } catch (java.nio.charset.CharacterCodingException ex) {
            throw new IllegalArgumentException("CSV must be encoded as UTF-8");
        }
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        List<List<String>> rows = parse(text);
        if (rows.isEmpty()) throw new IllegalArgumentException("CSV file is empty");
        List<String> headers = rows.getFirst().stream().map(String::trim).map(value -> value.toLowerCase(Locale.ROOT)).toList();
        if (!headers.contains("name")) throw new IllegalArgumentException("CSV must contain a name column");
        if (headers.size() != new HashSet<>(headers).size()) throw new IllegalArgumentException("CSV contains duplicate columns");
        for (String header : headers) {
            if (!SUPPORTED_HEADERS.contains(header)) throw new IllegalArgumentException("Unsupported CSV column: " + header);
        }
        if (rows.size() - 1 > MAX_ROWS) throw new IllegalArgumentException("CSV cannot contain more than " + MAX_ROWS + " data rows");

        List<ImportIssue> rejected = new ArrayList<>();
        int accepted = 0;
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            if (row.size() == 1 && row.getFirst().isBlank()) continue;
            if (row.size() != headers.size()) {
                rejected.add(new ImportIssue(rowIndex + 1, "The number of fields does not match the header"));
                continue;
            }
            CustomerRequest request;
            try {
                request = request(headers, row);
                var violations = validator.validate(request);
                if (!violations.isEmpty()) {
                    var violation = violations.iterator().next();
                    rejected.add(new ImportIssue(rowIndex + 1, violation.getPropertyPath() + ": " + violation.getMessage()));
                    continue;
                }
                creator.apply(request);
                accepted++;
            } catch (IllegalArgumentException ex) {
                rejected.add(new ImportIssue(rowIndex + 1, ex.getMessage()));
            }
        }
        return new ImportReport(rows.size() - 1, accepted, rejected.size(), rejected);
    }

    public byte[] exportCsv(String keyword, String contactType, String category, String segment, Long assignedEmployeeId) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        append(csv, HEADERS);
        int pageNumber = 0;
        Page<CustomerResponse> page;
        do {
            page = customers.getAllCustomersPaged(pageNumber++, 100, "id", "asc", keyword,
                    contactType, category, segment, assignedEmployeeId);
            for (CustomerResponse customer : page.getContent()) {
                append(csv, List.of(
                        safe(customer.getName()), safe(customer.getEmail()), safe(customer.getPhone()),
                        safe(customer.getCompany()), safe(customer.getAddress()), safe(customer.getContactType()),
                        safe(customer.getCategory()), safe(customer.getSegment()),
                        customer.getAssignedEmployeeId() == null ? "" : customer.getAssignedEmployeeId().toString()));
            }
        } while (page.hasNext());
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportXlsx(String keyword, String contactType, String category, String segment, Long assignedEmployeeId) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(HEADERS);
        int pageNumber = 0;
        Page<CustomerResponse> page;
        do {
            page = customers.getAllCustomersPaged(pageNumber++, 100, "id", "asc", keyword,
                    contactType, category, segment, assignedEmployeeId);
            for (CustomerResponse customer : page.getContent()) {
                rows.add(List.of(safe(customer.getName()), safe(customer.getEmail()), safe(customer.getPhone()),
                        safe(customer.getCompany()), safe(customer.getAddress()), safe(customer.getContactType()),
                        safe(customer.getCategory()), safe(customer.getSegment()),
                        customer.getAssignedEmployeeId() == null ? "" : customer.getAssignedEmployeeId().toString()));
            }
        } while (page.hasNext());
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            put(zip, "[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
            put(zip, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            put(zip, "xl/workbook.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Contacts\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            put(zip, "xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
            StringBuilder sheet = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                sheet.append("<row r=\"").append(rowIndex + 1).append("\">");
                List<String> row = rows.get(rowIndex);
                for (int col = 0; col < row.size(); col++) {
                    sheet.append("<c r=\"").append(columnName(col + 1)).append(rowIndex + 1).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                            .append(xml(row.get(col))).append("</t></is></c>");
                }
                sheet.append("</row>");
            }
            sheet.append("</sheetData></worksheet>");
            put(zip, "xl/worksheets/sheet1.xml", sheet.toString());
            zip.finish();
            return bytes.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create Excel export", ex);
        }
    }

    private void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String columnName(int column) {
        StringBuilder name = new StringBuilder();
        while (column > 0) { int remainder = (column - 1) % 26; name.insert(0, (char) ('A' + remainder)); column = (column - 1) / 26; }
        return name.toString();
    }

    private String xml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private CustomerRequest request(List<String> headers, List<String> row) {
        CustomerRequest request = new CustomerRequest();
        for (int i = 0; i < headers.size(); i++) {
            String value = row.get(i).trim();
            if (value.isEmpty()) value = null;
            switch (headers.get(i)) {
                case "name" -> request.setName(value);
                case "email" -> request.setEmail(value);
                case "phone" -> request.setPhone(value);
                case "company" -> request.setCompany(value);
                case "address" -> request.setAddress(value);
                case "contacttype" -> request.setContactType(value);
                case "category" -> request.setCategory(value);
                case "segment" -> request.setSegment(value);
                case "assignedemployeeid" -> {
                    if (value != null) {
                        try { request.setAssignedEmployeeId(Long.valueOf(value)); }
                        catch (NumberFormatException ex) { throw new IllegalArgumentException("assignedEmployeeId must be a positive integer"); }
                        if (request.getAssignedEmployeeId() <= 0) throw new IllegalArgumentException("assignedEmployeeId must be a positive integer");
                    }
                }
                default -> throw new IllegalArgumentException("Unsupported CSV column: " + headers.get(i));
            }
        }
        return request;
    }

    private List<List<String>> parse(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean afterQuote = false;
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (quoted) {
                if (current == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                        afterQuote = true;
                    }
                } else {
                    field.append(current);
                }
            } else if (current == ',') {
                row.add(field.toString());
                field.setLength(0);
                afterQuote = false;
            } else if (current == '\n' || current == '\r') {
                if (current == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
                afterQuote = false;
            } else if (current == '"' && field.isEmpty() && !afterQuote) {
                quoted = true;
            } else if (afterQuote || current == '"') {
                throw new IllegalArgumentException("Malformed CSV quoting near character " + (i + 1));
            } else {
                field.append(current);
            }
        }
        if (quoted) throw new IllegalArgumentException("CSV contains an unterminated quoted field");
        if (!field.isEmpty() || !row.isEmpty() || text.isEmpty() || (!text.endsWith("\n") && !text.endsWith("\r"))) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }

    private void append(StringBuilder csv, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) csv.append(',');
            String value = values.get(i) == null ? "" : values.get(i);
            csv.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }

    private String safe(String value) {
        if (!StringUtils.hasText(value)) return "";
        String trimmed = value.stripLeading();
        return !trimmed.isEmpty() && "=+-@\t\r".indexOf(trimmed.charAt(0)) >= 0 ? "'" + value : value;
    }

    public record ImportIssue(int row, String message) { }
    public record ImportReport(int totalRows, int acceptedRows, int rejectedRows, List<ImportIssue> issues) { }
}
