package com.hify.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果，字段对齐 CLAUDE.md 规范：list / total / page / pageSize
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    private List<T> list;

    /** 总记录数 */
    private Long total;

    /** 当前页码（从 1 开始） */
    private Long page;

    /** 每页条数 */
    private Long pageSize;

    public static <T> PageResult<T> ok(List<T> list, Long total, Long page, Long pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPage(page);
        result.setPageSize(pageSize);
        return result;
    }
}
