package com.hify.mcp.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class HttpJsonRpcTransport implements McpTransport {

    private static final RestTemplate REST_TEMPLATE = createRestTemplate();

    private final String url;
    private final Map<String, String> authHeaders;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger requestId = new AtomicInteger(1);

    public HttpJsonRpcTransport(String url, String authConfig) {
        this.url = url;
        this.authHeaders = parseAuthConfig(authConfig);
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        return new RestTemplate(factory);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseAuthConfig(String authConfig) {
        if (authConfig == null || authConfig.isBlank()) return Map.of();
        try {
            JsonNode node = mapper.readTree(authConfig);
            if (node.has("headers") && node.get("headers").isObject()) {
                return mapper.convertValue(node.get("headers"), Map.class);
            }
            return Map.of();
        } catch (Exception e) {
            log.warn("Failed to parse MCP auth config: {}", authConfig, e);
            return Map.of();
        }
    }

    @Override
    public JsonNode send(String method, JsonNode params) throws Exception {
        ObjectNode request = mapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("method", method);
        request.put("id", requestId.getAndIncrement());
        if (params != null) request.set("params", params);

        String reqJson = mapper.writeValueAsString(request);
        log.debug("MCP >> {}", reqJson);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.forEach(headers::set);

        HttpEntity<String> entity = new HttpEntity<>(reqJson, headers);
        ResponseEntity<String> response = REST_TEMPLATE.postForEntity(url, entity, String.class);

        String respBody = response.getBody();
        if (respBody == null) throw new IOException("MCP empty response");

        log.debug("MCP << {}", respBody);
        JsonNode resp = mapper.readTree(respBody);
        if (resp.has("error")) {
            throw new IOException("MCP error: " + resp.get("error"));
        }
        return resp.get("result");
    }

    @Override
    public void close() {
        // stateless, nothing to close
    }
}
