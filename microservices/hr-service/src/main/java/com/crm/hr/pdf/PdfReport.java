package com.crm.hr.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Stateless, in-memory A4 rendering. No file, database or remote resource access. */
public final class PdfReport {
    public record Row(String label, String value) {}
    private static final Color NAVY = new Color(8, 43, 73);
    private static final Color TEAL = new Color(24, 221, 210);
    private static final Color LIGHT = new Color(245, 248, 252);
    private final PDDocument document;
    private final PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final String title;
    private final String reference;
    private PDPageContentStream stream;
    private float y;
    private int page;

    private PdfReport(PDDocument document, String title, String reference) {
        this.document = document; this.title = title; this.reference = reference;
    }
    public static byte[] render(String title, String reference, List<Row> rows) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.getDocumentInformation().setTitle("CRM Workspace - " + title);
            doc.getDocumentInformation().setProducer("CRM Workspace");
            PdfReport report = new PdfReport(doc, title, reference);
            report.newPage();
            for (Row row : rows) report.row(row);
            report.finishPage();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException ex) { throw new UncheckedIOException("PDF generation failed", ex); }
    }
    public static ResponseEntity<byte[]> attachment(byte[] bytes, String filename) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .header(HttpHeaders.PRAGMA, "no-cache")
            .header(HttpHeaders.VARY, HttpHeaders.AUTHORIZATION)
            .header("X-Content-Type-Options", "nosniff")
            .contentLength(bytes.length).body(bytes);
    }
    public static void requireAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream().noneMatch(a ->
            a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN")))
            throw new AccessDeniedException("Administrative access required");
    }
    public static String value(Object value) {
        return value == null || value.toString().isBlank() ? "Non renseigné" : value.toString();
    }
    public static String date(LocalDate date) {
        return date == null ? value(null) : date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
    public static String amount(BigDecimal amount, String currency) {
        if (amount == null) return value(null);
        NumberFormat format = NumberFormat.getNumberInstance(Locale.FRANCE);
        format.setMinimumFractionDigits(2); format.setMaximumFractionDigits(340);
        return format.format(amount) + (currency == null || currency.isBlank() ? " (devise non renseignée)" : " " + currency);
    }
    private void newPage() throws IOException {
        if (stream != null) finishPage();
        PDPage sheet = new PDPage(PDRectangle.A4); document.addPage(sheet); page++;
        stream = new PDPageContentStream(document, sheet);
        float width = PDRectangle.A4.getWidth(), height = PDRectangle.A4.getHeight();
        rectangle(0, height - 100, width, 100, NAVY);
        rectangle(48, height - 105, width - 96, 4, TEAL);
        text("CRM WORKSPACE", 48, height - 30, 10, bold, TEAL);
        text(title, 48, height - 58, 20, bold, Color.WHITE);
        text(reference, 48, height - 80, 10, regular, Color.WHITE);
        y = height - 138;
        rectangle(48, y - 7, width - 96, 23, NAVY);
        text("Rubrique", 56, y, 10, bold, Color.WHITE);
        text("Donnée enregistrée", 218, y, 10, bold, Color.WHITE);
        y -= 27;
    }
    private void row(Row row) throws IOException {
        List<String> labels = wrap(row.label(), 150), values = wrap(value(row.value()), 320);
        int count = Math.max(labels.size(), values.size());
        for (int i = 0; i < count; i++) {
            boolean continued = y < 72;
            if (continued) newPage();
            rectangle(48, y - 5, PDRectangle.A4.getWidth() - 96, 18, LIGHT);
            String label = i < labels.size() ? labels.get(i) : continued ? labels.getFirst() + " (suite)" : "";
            text(label, 56, y, 9.5f, regular, NAVY);
            if (i < values.size()) text(values.get(i), 218, y, 9.5f, regular, NAVY);
            y -= 18;
        }
        y -= 6;
    }
    private List<String> wrap(String raw, float width) throws IOException {
        String cleaned = printable(value(raw));
        List<String> result = new ArrayList<>();
        for (String paragraph : cleaned.split("\n", -1)) {
            String line = "";
            for (int code : paragraph.codePoints().toArray()) {
                String next = line + new String(Character.toChars(code));
                if (!line.isEmpty() && regular.getStringWidth(next) / 1000 * 9.5f > width) {
                    int space = line.lastIndexOf(' ');
                    if (space > 0) { result.add(line.substring(0, space)); line = line.substring(space + 1); }
                    else { result.add(line); line = ""; }
                }
                line += new String(Character.toChars(code));
            }
            result.add(line);
        }
        return result;
    }
    private String printable(String raw) {
        StringBuilder safe = new StringBuilder();
        raw.replace("\r\n", "\n").codePoints().forEach(code -> {
            if (code == '\n') { safe.append('\n'); return; }
            String ch = code == 0x202f || code == 0xa0 || Character.isISOControl(code) ? " " : new String(Character.toChars(code));
            try { regular.encode(ch); safe.append(ch); }
            catch (IllegalArgumentException | IOException unsupported) { safe.append('?'); }
        });
        return safe.toString();
    }
    private void text(String raw, float x, float y, float size, PDFont font, Color color) throws IOException {
        stream.beginText(); stream.setFont(font, size); stream.setNonStrokingColor(color);
        stream.newLineAtOffset(x, y); stream.showText(printable(raw)); stream.endText();
    }
    private void rectangle(float x, float y, float w, float h, Color color) throws IOException {
        stream.setNonStrokingColor(color); stream.addRect(x, y, w, h); stream.fill();
    }
    private void finishPage() throws IOException {
        text("CRM Workspace · Données enregistrées · Page " + page, 48, 35, 9, regular, NAVY);
        stream.close(); stream = null;
    }
}
