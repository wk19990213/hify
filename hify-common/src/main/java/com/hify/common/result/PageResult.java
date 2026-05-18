package com.hify.common.result;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应结果
 *
 * @param <T> 数据类型
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PageResult<T> extends Result<List<T>> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Long page;

    /**
     * 每页大小
     */
    private Long size;

    /**
     * 成功响应（带分页数据）
     */
    public static <T> PageResult<T> ok(List<T> data, Long total, Long page, Long size) {
        PageResult<T> result = new PageResult<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        return result;
    }

    /**
     * 成功响应（带分页数据，page/size 为 Integer 类型）
     */
    public static <T> PageResult<T> ok(List<T> data, Long total, Integer page, Integer size) {
        return ok(data, total, Long.valueOf(page), Long.valueOf(size));
    }
}
