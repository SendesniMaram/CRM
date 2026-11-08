package com.crm.invoice;

import com.crm.invoice.service.PersonalInvoiceService.MoneySummary;
import com.crm.invoice.service.PersonalInvoiceService.PersonalStatistics;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** These exact JSON fixtures are also consumed by Angular's DashboardDataService tests. */
class ClientStatisticsContractTest {
    @Test void javaSerializerMatchesTheSharedAngularContract() throws Exception {
        var json = new ObjectMapper();
        var fixture = Path.of("../../frontend/src/app/core/testing/client-invoice-statistics.json");
        var samples = json.readTree(fixture.toFile());
        assertEquals(4, samples.size());
        for (var sample : samples) {
            var currencies = new TreeMap<String, MoneySummary>();
            sample.path("byCurrency").fields().forEachRemaining(entry -> {
                var row = entry.getValue();
                currencies.put(entry.getKey(), new MoneySummary(row.path("invoiceCount").longValue(),
                        new BigDecimal(row.path("total").asText()), new BigDecimal(row.path("paid").asText()),
                        new BigDecimal(row.path("outstanding").asText())));
            });
            var dto = new PersonalStatistics(sample.path("invoiceCount").longValue(),
                    sample.path("paidInvoiceCount").longValue(), sample.path("unpaidInvoiceCount").longValue(), currencies);
            var serialized = json.readTree(json.writeValueAsString(dto));
            assertEquals(sample, serialized);
            assertTrue(serialized.path("byCurrency").isObject());
        }
    }
}
