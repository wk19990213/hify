package com.hify.common.dto;

import lombok.Data;

/**
 * 分页查询基类，定义通用分页+筛选字段。
 * 模块 ListParams 继承此类，仅添加模块特有字段。
 */
@Data
public class BasePageParams {
    private Integer page = 1;
    private Integer pageSize = 20;
    private String name;
    private Integer status;
    private String sortField;
    private String sortOrder;
}
