package com.crm.invoice.config;import org.springframework.context.annotation.*;import java.time.Clock;
@Configuration public class InvoiceClockConfiguration {@Bean public Clock invoiceClock(){return Clock.systemUTC();}}
