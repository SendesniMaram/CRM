package com.crm.hr.client;

import com.crm.hr.dto.EmployeeProfileSummary;
import com.crm.hr.exception.EmployeeProfileException;
import com.crm.hr.security.EmployeeServiceTokenSigner;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.*;

/** Dedicated HTTPS destination and credentials. No user bearer, cache or retry. */
public final class EmployeeProfileClient implements AutoCloseable {
    record Reply(int status, byte[] body, String contentType) {}
    @FunctionalInterface interface Exchange { Reply get(String employeeId, String serviceToken) throws Exception; }
    private final EmployeeProfileClientProperties properties;
    private final EmployeeServiceTokenSigner signer;
    private final Exchange exchange;
    private final AutoCloseable transport;
    private final ObjectMapper json = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private final ExecutorService workers = new ThreadPoolExecutor(0, 4, 30, TimeUnit.SECONDS, new SynchronousQueue<>(), task -> {
        var thread = new Thread(task, "employee-profile"); thread.setDaemon(true); return thread;
    }, new ThreadPoolExecutor.AbortPolicy());

    EmployeeProfileClient(EmployeeProfileClientProperties properties, EmployeeServiceTokenSigner signer, Exchange exchange, AutoCloseable transport) {
        this.properties = properties; this.signer = signer; this.exchange = exchange; this.transport = transport;
    }
    public EmployeeProfileSummary read(String employeeId) {
        if (!canonical(employeeId)) throw invalid();
        if (!properties.enabled() || signer == null || exchange == null) throw unavailable();
        Future<EmployeeProfileSummary> pending = null;
        try {
            pending = workers.submit(() -> {
                Reply reply = exchange.get(employeeId, signer.signForEmployee(employeeId));
                if (reply.status() == 404) throw new EmployeeProfileException(404, "PROFILE_NOT_FOUND");
                if (reply.status() == 401 || reply.status() == 403 || reply.status() >= 500) throw unavailable();
                if (reply.status() != 200) throw invalid();
                return decode(reply, employeeId);
            });
            return pending.get(properties.totalTimeout().toNanos(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); throw unavailable();
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof EmployeeProfileException failure) throw failure;
            throw unavailable();
        } catch (TimeoutException | RejectedExecutionException ex) { throw unavailable();
        } finally { if (pending != null && !pending.isDone()) pending.cancel(true); }
    }
    private EmployeeProfileSummary decode(Reply reply, String expected) {
        try {
            if (reply.body() == null || reply.body().length > 16384 || reply.contentType() == null
                    || !reply.contentType().split(";", 2)[0].trim().equalsIgnoreCase("application/json")) throw invalid();
            JsonNode body = json.readTree(reply.body());
            var fields = new java.util.HashSet<String>();
            if (body == null || !body.isObject()) throw invalid();
            body.fieldNames().forEachRemaining(fields::add);
            if (!fields.equals(Set.of("employeeId", "firstName", "lastName", "jobTitle", "departmentId", "hireDate"))
                    || !body.path("employeeId").isTextual() || !expected.equals(body.path("employeeId").textValue())
                    || !canonical(body.path("employeeId").textValue())) throw invalid();
            String department = nullableText(body.get("departmentId"));
            if (department != null && !canonical(department)) throw invalid();
            String hire = nullableText(body.get("hireDate"));
            if (hire != null && !hire.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw invalid();
            return new EmployeeProfileSummary(expected, nullableText(body.get("firstName")), nullableText(body.get("lastName")),
                    nullableText(body.get("jobTitle")), department, hire == null ? null : LocalDate.parse(hire));
        } catch (Exception ex) { throw invalid(); }
    }
    private static String nullableText(JsonNode value) {
        if (value != null && value.isNull()) return null;
        if (value == null || !value.isTextual() || value.textValue().length() > 500) throw invalid();
        return value.textValue();
    }
    static boolean canonical(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) return false;
        try { return Long.parseLong(value) > 0; } catch (NumberFormatException ex) { return false; }
    }
    private static EmployeeProfileException unavailable() { return new EmployeeProfileException(503, "PROFILE_SERVICE_UNAVAILABLE"); }
    private static EmployeeProfileException invalid() { return new EmployeeProfileException(502, "PROFILE_INVALID_RESPONSE"); }
    @Override public void close() {
        workers.shutdownNow();
        if (transport != null) try { transport.close(); } catch (Exception ignored) { /* No credentials or transport details logged. */ }
    }
}
