package com.hify.mcp.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class SseTransport implements McpTransport {
    private final String url;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger requestId = new AtomicInteger(1);

    public SseTransport(String url) {
        this.url = url;
    }

    @Override
    public JsonNode send(String method, JsonNode params) throws Exception {
        ObjectNode request = mapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("method", method);
        request.put("id", requestId.getAndIncrement());
        if (params != null) request.set("params", params);

        byte[] body = mapper.writeValueAsBytes(request);
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
        }

        int status = conn.getResponseCode();
        InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String respBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        JsonNode resp = mapper.readTree(respBody);
        if (resp.has("error")) {
            throw new IOException("MCP error: " + resp.get("error"));
        }
        return resp.get("result");
    }

    @Override
    public void close() {
        // SSE transport is stateless, nothing to close
    }
}
