package com.hify.mcp.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class StdioTransport implements McpTransport {
    private final List<String> command;
    private Process process;
    private BufferedWriter writer;
    private BufferedReader reader;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger requestId = new AtomicInteger(1);

    public StdioTransport(String command, List<String> args) {
        this.command = new java.util.ArrayList<>();
        this.command.add(command);
        if (args != null) this.command.addAll(args);
    }

    @Override
    public JsonNode send(String method, JsonNode params) throws Exception {
        ensureProcess();
        ObjectNode request = mapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("method", method);
        request.put("id", requestId.getAndIncrement());
        if (params != null) request.set("params", params);

        String reqJson = mapper.writeValueAsString(request);
        log.debug("MCP >> {}", reqJson);
        writer.write(reqJson);
        writer.newLine();
        writer.flush();

        String line = reader.readLine();
        if (line == null) throw new IOException("MCP process closed unexpectedly");
        log.debug("MCP << {}", line);
        JsonNode resp = mapper.readTree(line);
        if (resp.has("error")) {
            throw new IOException("MCP error: " + resp.get("error"));
        }
        return resp.get("result");
    }

    private void ensureProcess() throws IOException {
        if (process != null && process.isAlive()) return;
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);
        process = pb.start();
        writer = new BufferedWriter(new OutputStreamWriter(
                process.getOutputStream(), StandardCharsets.UTF_8));
        reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public void close() {
        if (process != null) {
            process.destroy();
            try { process.waitFor(); } catch (InterruptedException ignored) {}
        }
    }
}
