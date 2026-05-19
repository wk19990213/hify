package com.hify.agent.controller;

import com.hify.agent.dto.AgentListParams;
import com.hify.agent.dto.AgentRequest;
import com.hify.agent.dto.AgentResponse;
import com.hify.agent.service.AgentService;
import com.hify.common.result.PageResult;
import com.hify.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Agent 控制器
 */
@RestController
@RequestMapping("/v1/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody AgentRequest req) {
        return Result.ok(agentService.create(req));
    }

    @GetMapping
    public Result<PageResult<AgentResponse>> list(AgentListParams params) {
        return Result.ok(agentService.list(params));
    }

    @GetMapping("/{id}")
    public Result<AgentResponse> getDetail(@PathVariable("id") Long id) {
        return Result.ok(agentService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
            @Valid @RequestBody AgentRequest req) {
        agentService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        agentService.delete(id);
        return Result.ok();
    }

    @PostMapping("/batch-status")
    public Result<Void> batchUpdateStatus(@RequestBody BatchStatusRequest req) {
        agentService.batchUpdateStatus(req.getIds(), req.getStatus());
        return Result.ok();
    }

    /**
     * 批量状态更新请求
     */
    @lombok.Data
    public static class BatchStatusRequest {
        private List<Long> ids;
        private Integer status;
    }
}
