package com.hify.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DemoItemCreateReq {

    @NotBlank(message = "名称不能为空")
    private String name;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
