package com.hify.mcp.controller;

import com.hify.common.result.PageResult;
import com.hify.common.result.Result;
import com.hify.mcp.dto.*;
import com.hify.mcp.service.McpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/mcp-servers")
@RequiredArgsConstructor
public class McpController {

    private final McpService mcpService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody McpServerCreateReq req) {
        return Result.ok(mcpService.create(req));
    }

    @GetMapping
    public Result<PageResult<McpServerResp>> list(McpServerListParams params) {
        return Result.ok(mcpService.list(params));
    }

    @GetMapping("/{id}")
    public Result<McpServerResp> getDetail(@PathVariable Long id) {
        return Result.ok(mcpService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody McpServerUpdateReq req) {
        mcpService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        mcpService.delete(id);
        return Result.ok();
    }
}
