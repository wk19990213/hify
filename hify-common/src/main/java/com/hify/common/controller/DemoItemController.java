package com.hify.common.controller;

import com.hify.common.dto.DemoItemCreateReq;
import com.hify.common.dto.DemoItemResp;
import com.hify.common.dto.DemoItemUpdateReq;
import com.hify.common.result.PageResult;
import com.hify.common.result.Result;
import com.hify.common.service.DemoItemService;
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

@RestController
@RequestMapping("/v1/demo-items")
@RequiredArgsConstructor
public class DemoItemController {

    private final DemoItemService demoItemService;

    @GetMapping
    public PageResult<DemoItemResp> list(
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize) {
        return demoItemService.list(page, pageSize);
    }

    @GetMapping("/{id}")
    public Result<DemoItemResp> getById(@PathVariable(name = "id") Long id) {
        DemoItemResp resp = demoItemService.getById(id);
        return Result.ok(resp);
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody DemoItemCreateReq req) {
        Long id = demoItemService.create(req.getName(), req.getStatus());
        return Result.ok(id);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable(name = "id") Long id, @Valid @RequestBody DemoItemUpdateReq req) {
        demoItemService.update(id, req.getName(), req.getStatus());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable(name = "id") Long id) {
        demoItemService.delete(id);
        return Result.ok();
    }
}
