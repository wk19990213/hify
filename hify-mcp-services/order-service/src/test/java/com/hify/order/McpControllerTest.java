package com.hify.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class McpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldInitializeAndReturnServerInfo() throws Exception {
        String req = """
            {"jsonrpc":"2.0","method":"initialize","id":1,"params":{
              "protocolVersion":"2024-11-05",
              "capabilities":{"tools":{}},
              "clientInfo":{"name":"test","version":"1.0"}
            }}""";

        mockMvc.perform(post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jsonrpc").value("2.0"))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.result.protocolVersion").value("2024-11-05"))
            .andExpect(jsonPath("$.result.capabilities.tools").isMap())
            .andExpect(jsonPath("$.result.serverInfo.name").value("order-server"));
    }

    @Test
    void shouldListTools() throws Exception {
        String req = """
            {"jsonrpc":"2.0","method":"tools/list","id":2,"params":{"page":1,"pageSize":10}}""";

        mockMvc.perform(post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jsonrpc").value("2.0"))
            .andExpect(jsonPath("$.id").value(2))
            .andExpect(jsonPath("$.result.tools.length()").value(2))
            .andExpect(jsonPath("$.result.tools[0].name").isString())
            .andExpect(jsonPath("$.result.tools[1].name").isString())
            .andExpect(jsonPath("$.result.total").value(2));
    }

    @Test
    void shouldCallQueryOrder() throws Exception {
        String req = """
            {"jsonrpc":"2.0","method":"tools/call","id":3,"params":{
              "name":"query_order",
              "arguments":{"orderId":"12345"}
            }}""";

        mockMvc.perform(post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jsonrpc").value("2.0"))
            .andExpect(jsonPath("$.id").value(3))
            .andExpect(jsonPath("$.result.content[0].type").value("text"))
            .andExpect(jsonPath("$.result.content[0].text").isString());
    }

    @Test
    void shouldReturnErrorForUnknownTool() throws Exception {
        String req = """
            {"jsonrpc":"2.0","method":"tools/call","id":4,"params":{
              "name":"unknown_tool",
              "arguments":{}
            }}""";

        mockMvc.perform(post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jsonrpc").value("2.0"))
            .andExpect(jsonPath("$.id").value(4))
            .andExpect(jsonPath("$.result.content[0].text").isString());
    }

    @Test
    void shouldReturnErrorForUnknownMethod() throws Exception {
        String req = """
            {"jsonrpc":"2.0","method":"unknown/method","id":5,"params":{}}""";

        mockMvc.perform(post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.error.code").value(-32601));
    }
}
