package com.hify.controller;

import com.hify.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器
 */
@RestController
public class HealthController {

    @GetMapping("/")
    public Result<Void> root() {
        return Result.ok();
    }

    @GetMapping("/v1/health")
    public Result<Void> health() {
        return Result.ok();
    }
}
