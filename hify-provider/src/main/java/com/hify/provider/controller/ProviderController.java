package com.hify.provider.controller;

import com.hify.common.result.PageResult;
import com.hify.common.result.Result;
import com.hify.provider.dto.ConnectionTestResult;
import com.hify.provider.dto.ProviderCreateReq;
import com.hify.provider.dto.ProviderDetailResp;
import com.hify.provider.dto.ProviderResp;
import com.hify.provider.dto.ProviderUpdateReq;
import com.hify.provider.service.ProviderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 模型提供商控制器
 */
@RestController
@RequestMapping("/v1/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderService providerService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody ProviderCreateReq req) {
        return Result.ok(providerService.create(req));
    }

    @GetMapping
    public Result<PageResult<ProviderResp>> list(
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize) {
        return Result.ok(providerService.list(page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<ProviderDetailResp> getDetail(@PathVariable Long id) {
        return Result.ok(providerService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProviderUpdateReq req) {
        req.setId(id);
        providerService.update(req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        providerService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/test-connection")
    public Result<ConnectionTestResult> testConnection(@PathVariable Long id) {
        return Result.ok(providerService.testConnection(id));
    }
}
