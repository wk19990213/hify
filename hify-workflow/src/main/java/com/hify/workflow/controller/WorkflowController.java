package com.hify.workflow.controller;

import com.hify.common.result.PageResult;
import com.hify.common.result.Result;
import com.hify.workflow.dto.*;
import com.hify.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody WorkflowCreateReq req) {
        return Result.ok(workflowService.create(req));
    }

    @GetMapping
    public Result<PageResult<WorkflowResp>> list(WorkflowListParams params) {
        return Result.ok(workflowService.list(params));
    }

    @GetMapping("/{id}")
    public Result<WorkflowResp> getDetail(@PathVariable Long id) {
        return Result.ok(workflowService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody WorkflowUpdateReq req) {
        workflowService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        workflowService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/run")
    public Result<WorkflowInstanceResp> run(@PathVariable Long id, @RequestBody WorkflowRunReq req) {
        return Result.ok(workflowService.run(id, req));
    }

    @GetMapping("/runs")
    public Result<PageResult<WorkflowInstanceResp>> listInstances(
            @RequestParam(required = false) Long workflowId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(workflowService.listInstances(workflowId, page, pageSize));
    }

    @GetMapping("/runs/{id}")
    public Result<WorkflowInstanceResp> getInstanceDetail(@PathVariable Long id) {
        return Result.ok(workflowService.getInstanceDetail(id));
    }
}
