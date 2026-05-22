package com.hify.mcp.mcp;

import com.fasterxml.jackson.databind.JsonNode;

public interface McpTransport extends AutoCloseable {
    /** 发送 JSON-RPC 请求，返回响应 */
    JsonNode send(String method, JsonNode params) throws Exception;
}
