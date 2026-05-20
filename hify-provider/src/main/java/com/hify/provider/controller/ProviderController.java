package com.hify.provider.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.common.result.PageResult;
import com.hify.common.result.Result;
import com.hify.provider.dto.ConnectionTestResult;
import com.hify.provider.dto.ProviderRequest;
import com.hify.provider.dto.ProviderResponse;
import com.hify.provider.entity.ModelConfigEntity;
import com.hify.provider.mapper.ModelConfigMapper;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型提供商控制器
 */
@RestController
@RequestMapping("/v1/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderService providerService;
    private final ModelConfigMapper modelConfigMapper;

    /** 获取所有已启用的模型配置（供下拉选择） */
    @GetMapping("/model-configs")
    public Result<List<Map<String, Object>>> listModelConfigs() {
        List<Map<String, Object>> list = modelConfigMapper.selectList(
                        new LambdaQueryWrapper<ModelConfigEntity>()
                                .eq(ModelConfigEntity::getStatus, 1)
                                .eq(ModelConfigEntity::getDeleted, 0)
                                .orderByAsc(ModelConfigEntity::getSortOrder))
                .stream()
                .map(m -> {
                    Map<String, Object> item = new java.util.HashMap<>();
                    item.put("id", m.getId());
                    item.put("name", m.getName());
                    item.put("providerId", m.getProviderId());
                    return item;
                })
                .toList();
        return Result.ok(list);
    }

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
