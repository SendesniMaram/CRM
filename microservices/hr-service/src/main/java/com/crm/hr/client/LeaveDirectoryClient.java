package com.crm.hr.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Uses existing read endpoints with the administrator's bearer header. */
@Component
public class LeaveDirectoryClient {
    private final DiscoveryClient discovery;
    public LeaveDirectoryClient(DiscoveryClient discovery) { this.discovery = discovery; }
    public JsonNode read(String service, String path, String authorization) {
        var instances = discovery.getInstances(service);
        if (instances.isEmpty()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Leave directory unavailable");
        try {
            var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory() {
                @Override protected void prepareConnection(java.net.HttpURLConnection connection, String method) throws java.io.IOException {
                    super.prepareConnection(connection, method);
                    connection.setInstanceFollowRedirects(false);
                }
            };
            factory.setProxy(java.net.Proxy.NO_PROXY);
            factory.setConnectTimeout(5000);
            factory.setReadTimeout(5000);
            JsonNode response = RestClient.builder().baseUrl(instances.get(0).getUri().toString()).requestFactory(factory).build().get().uri(path)
                    .header(HttpHeaders.AUTHORIZATION, authorization).retrieve()
                    .onStatus(status -> status.is3xxRedirection(), (request, remoteResponse) -> {
                        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Leave directory redirect refused");
                    }).body(JsonNode.class);
            if (response == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid leave directory response");
            return response;
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 401) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Leave directory authentication failed");
            if (status == 403) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Leave directory access denied");
            if (status == 404) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Directory resource not found");
            throw new ResponseStatusException(status >= 500 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY,
                    "Leave directory unavailable");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Leave directory unavailable");
        }
    }
}
