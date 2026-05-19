package com.hify.provider.controller;

import com.hify.common.result.PageResult;
import com.hify.common.result.Result;
import com.hify.provider.dto.ConnectionTestResult;
import com.hify.provider.dto.ProviderRequest;
import com.hify.provider.dto.ProviderResponse;
import com.hify.provider.service.ProviderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模型提供商控制器
 */
@RestController
@RequestMapping("/v1/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderService providerService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody ProviderRequest req) {
        return Result.ok(providerService.create(req));
    }

    @GetMapping
    public Result<PageResult<ProviderResponse>> list(
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize) {
        return Result.ok(providerService.list(page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<ProviderResponse> getDetail(@PathVariable("id") Long id) {
        return Result.ok(providerService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
            @Valid @RequestBody ProviderRequest req) {
        providerService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        providerService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/test-connection")
    public Result<ConnectionTestResult> testConnection(@PathVariable("id") Long id) {
        return Result.ok(providerService.testConnection(id));
    }
}
