package com.crm.payroll.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.util.regex.Pattern;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/** Resolves the employee from the existing HR association using the caller's validated bearer. */
@Component
public class PayrollEmployeeDirectoryClient {
    private static final Pattern ID = Pattern.compile("[1-9][0-9]{0,18}");
    private final DiscoveryClient discovery;

    public PayrollEmployeeDirectoryClient(DiscoveryClient discovery) {
        this.discovery = discovery;
    }

    public String ownEmployeeId(String authorization, long authenticatedUserId) {
        var instances = discovery.getInstances("hr-service");
        if (instances.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Employee association service unavailable");
        }
        try {
            var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(HttpURLConnection connection, String method) throws java.io.IOException {
                    super.prepareConnection(connection, method);
                    connection.setInstanceFollowRedirects(false);
                }
            };
            factory.setProxy(Proxy.NO_PROXY);
            factory.setConnectTimeout(5000);
            factory.setReadTimeout(5000);
            JsonNode response = RestClient.builder()
                    .baseUrl(instances.get(0).getUri().toString())
                    .requestFactory(factory)
                    .build()
                    .get()
                    .uri("/api/employee-associations/me")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .onStatus(status -> status.is3xxRedirection(), (request, remoteResponse) -> {
                        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Employee association redirect refused");
                    })
                    .body(JsonNode.class);
            if (response == null || !Long.toString(authenticatedUserId).equals(response.path("userId").asText(null))
                    || !canonicalId(response.path("employeeId").asText(null))) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid employee association response");
            }
            return response.path("employeeId").asText();
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 401) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Employee association authentication failed");
            if (status == 403) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Employee association access denied");
            if (status == 404) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee account is not associated");
            throw new ResponseStatusException(status >= 500 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY,
                    "Employee association service unavailable");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Employee association service unavailable");
        }
    }

    private static boolean canonicalId(String value) {
        return value != null && ID.matcher(value).matches();
    }
}
