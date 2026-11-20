package com.crm.invoice.config;

import com.crm.invoice.entity.InvoiceNumberSequence;
import com.crm.invoice.repository.InvoiceNumberSequenceRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InvoiceSequenceInitializer {
    @Bean ApplicationRunner initializeInvoiceSequence(InvoiceNumberSequenceRepository repository) {
        return args -> {
            if (!repository.existsById(1)) {
                var sequence = new InvoiceNumberSequence(); sequence.setId(1); sequence.setNextValue(1L);
                repository.save(sequence);
            }
        };
    }
}
