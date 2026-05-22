package com.hify.order.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hify.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/mcp")
public class McpController {

    private static final Logger log = LoggerFactory.getLogger(McpController.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final OrderService orderService;

    public McpController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public JsonNode handleRequest(@RequestBody JsonNode request) {
        String method = request.has("method") ? request.get("method").asText() : "";
        JsonNode idNode = request.has("id") ? request.get("id") : null;
        JsonNode params = request.has("params") ? request.get("params") : null;

        log.info("MCP request: method={}, id={}", method, idNode);

        return switch (method) {
            case "initialize" -> handleInitialize(idNode, params);
            case "tools/list" -> handleToolsList(idNode, params);
            case "tools/call" -> handleToolCall(idNode, params);
            default -> error(idNode, -32601, "Method not found: " + method);
        };
    }

    private JsonNode handleInitialize(JsonNode id, JsonNode params) {
        ObjectNode result = mapper.createObjectNode();
        result.put("protocolVersion", "2024-11-05");
        ObjectNode capabilities = mapper.createObjectNode();
        capabilities.putObject("tools");
        result.set("capabilities", capabilities);
        ObjectNode serverInfo = mapper.createObjectNode();
        serverInfo.put("name", "order-server");
        serverInfo.put("version", "1.0.0");
        result.set("serverInfo", serverInfo);
        return response(id, result);
    }

    private JsonNode handleToolsList(JsonNode id, JsonNode params) {
        int page = params != null && params.has("page") ? params.get("page").asInt(1) : 1;
        int pageSize = params != null && params.has("pageSize") ? params.get("pageSize").asInt(10) : 10;

        List<Map<String, Object>> allTools = List.of(
            buildToolDef("query_order", "根据订单号查询订单详情，返回订单状态、客户、商品明细、快递单号等信息",
                Map.of("type", "object",
                    "properties", Map.of("orderId", Map.of("type", "string", "description", "订单号，例如 12345")),
                    "required", List.of("orderId"))),
            buildToolDef("list_orders", "列出所有订单，可按状态过滤（已发货/待发货/已签收）",
                Map.of("type", "object",
                    "properties", Map.of("status", Map.of("type", "string", "description", "订单状态过滤，可选：已发货、待发货、已签收"))))
        );

        int total = allTools.size();
        int from = Math.min((page - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<Map<String, Object>> paged = allTools.subList(from, to);

        ObjectNode result = mapper.createObjectNode();
        result.set("tools", mapper.valueToTree(paged));
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return response(id, result);
    }

    private JsonNode handleToolCall(JsonNode id, JsonNode params) {
        String name = params != null && params.has("name") ? params.get("name").asText() : "";
        JsonNode arguments = params != null && params.has("arguments") ? params.get("arguments") : mapper.createObjectNode();

        String text;
        if ("query_order".equals(name)) {
            String orderId = arguments.has("orderId") ? arguments.get("orderId").asText() : "";
            var order = orderService.queryOrder(orderId);
            text = order.map(o -> {
                try { return mapper.writeValueAsString(o); }
                catch (Exception e) { return "{}"; }
            }).orElse("{\"error\":\"订单不存在: " + orderId + "\"}");
        } else if ("list_orders".equals(name)) {
            String status = arguments.has("status") ? arguments.get("status").asText() : null;
            var orders = orderService.listOrders(status);
            try { text = mapper.writeValueAsString(orders); }
            catch (Exception e) { text = "[]"; }
        } else {
            text = "{\"error\":\"未知工具: " + name + "\"}";
        }

        ObjectNode result = mapper.createObjectNode();
        ArrayNode content = mapper.createArrayNode();
        ObjectNode textContent = mapper.createObjectNode();
        textContent.put("type", "text");
        textContent.put("text", text);
        content.add(textContent);
        result.set("content", content);
        return response(id, result);
    }

    private Map<String, Object> buildToolDef(String name, String description, Map<String, Object> inputSchema) {
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("name", name);
        tool.put("description", description);
        tool.put("inputSchema", inputSchema);
        return tool;
    }

    private JsonNode response(JsonNode id, JsonNode result) {
        ObjectNode resp = mapper.createObjectNode();
        resp.put("jsonrpc", "2.0");
        if (id != null) resp.set("id", id);
        resp.set("result", result);
        return resp;
    }

    private JsonNode error(JsonNode id, int code, String message) {
        ObjectNode resp = mapper.createObjectNode();
        resp.put("jsonrpc", "2.0");
        if (id != null) resp.set("id", id);
        ObjectNode err = mapper.createObjectNode();
        err.put("code", code);
        err.put("message", message);
        resp.set("error", err);
        return resp;
    }
}
