package com.hify.common.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hify.common.result.PageResult;

public class PageHelper {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    /**
     * 前端分页参数转 MyBatis-Plus Page 对象
     *
     * @param page     页码（从1开始，null 或 <1 默认为1）
     * @param pageSize 每页条数（null 或 <1 默认为20，最大100）
     */
    public static <T> Page<T> toPage(Integer page, Integer pageSize) {
        int pageNum = (page == null || page < 1) ? DEFAULT_PAGE : page;
        int size = (pageSize == null || pageSize < 1) ? DEFAULT_SIZE : Math.min(pageSize, MAX_SIZE);
        return new Page<>(pageNum, size);
    }

    /**
     * MyBatis-Plus 分页结果转 PageResult
     */
    public static <T> PageResult<T> toPageResult(IPage<T> iPage) {
        return PageResult.ok(
                iPage.getRecords(),
                iPage.getTotal(),
                iPage.getCurrent(),
                iPage.getSize()
        );
    }
}
