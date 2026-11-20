package com.crm.invoice;
import com.crm.invoice.pdf.PdfReport;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class PdfPaginationTest {
    @Test void wrapsLongTextAndRepeatsHeadersWithoutLosingTheEnd() throws Exception {
        String notes = "Échéance et rémunération : texte très long. ".repeat(700) + "FIN-DU-DOCUMENT";
        var bytes = PdfReport.render("Facture", "TEST", List.of(new PdfReport.Row("Notes", notes)));
        try(var doc = Loader.loadPDF(bytes)) {
            assertTrue(doc.getNumberOfPages() > 2);
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("Échéance et rémunération"));
            assertTrue(text.contains("FIN-DU-DOCUMENT"));
            for(var page:doc.getPages()) assertEquals(841.89f,page.getMediaBox().getHeight(),.1f);
        }
    }
}
