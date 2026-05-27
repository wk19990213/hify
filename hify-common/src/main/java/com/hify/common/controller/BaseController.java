package com.hify.common.controller;

import com.hify.common.result.PageResult;
import com.hify.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 通用 CRUD Controller 基类。
 * 子类继承并实现 {@link #getService()} 即可获得标准 CRUD 端点。
 *
 * @param <S> Service 接口，需提供 create/update/delete/list/detail 方法
 * @param <R> 请求 DTO
 * @param <P> 响应 DTO
 */
public abstract class BaseController<S, R, P> {

    protected abstract S getService();

    protected abstract Long getCreateId(S service, R req);

    protected abstract P getDetail(S service, Long id);

    protected abstract PageResult<P> list(S service, Integer page, Integer pageSize);

    @PostMapping
    public Result<Long> create(@Valid @RequestBody R req) {
        return Result.ok(getCreateId(getService(), req));
    }

    @GetMapping
    public Result<PageResult<P>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(list(getService(), page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<P> detail(@PathVariable Long id) {
        return Result.ok(getDetail(getService(), id));
    }
}
