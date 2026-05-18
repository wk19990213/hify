package com.hify.common.service;

import com.hify.common.dto.DemoItemResp;
import com.hify.common.result.PageResult;

public interface DemoItemService {

    PageResult<DemoItemResp> list(Integer page, Integer pageSize);

    DemoItemResp getById(Long id);

    Long create(String name, Integer status);

    void update(Long id, String name, Integer status);

    void delete(Long id);
}
