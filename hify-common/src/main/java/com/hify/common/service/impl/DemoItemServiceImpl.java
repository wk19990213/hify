package com.hify.common.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hify.common.dto.DemoItemResp;
import com.hify.common.entity.DemoItem;
import com.hify.common.mapper.DemoItemMapper;
import com.hify.common.result.PageResult;
import com.hify.common.service.DemoItemService;
import com.hify.common.util.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DemoItemServiceImpl implements DemoItemService {

    private final DemoItemMapper demoItemMapper;

    @Override
    public PageResult<DemoItemResp> list(Integer page, Integer pageSize) {
        Page<DemoItem> pageObj = PageHelper.toPage(page, pageSize);
        IPage<DemoItem> iPage = demoItemMapper.selectPage(pageObj, null);
        IPage<DemoItemResp> resultPage = iPage.convert(this::toResp);
        return PageHelper.toPageResult(resultPage);
    }

    @Override
    public DemoItemResp getById(Long id) {
        DemoItem entity = demoItemMapper.selectById(id);
        return entity != null ? toResp(entity) : null;
    }

    @Override
    public Long create(String name, Integer status) {
        DemoItem entity = new DemoItem();
        entity.setName(name);
        entity.setStatus(status);
        demoItemMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Long id, String name, Integer status) {
        DemoItem entity = new DemoItem();
        entity.setId(id);
        entity.setName(name);
        entity.setStatus(status);
        demoItemMapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        demoItemMapper.deleteById(id);
    }

    private DemoItemResp toResp(DemoItem entity) {
        DemoItemResp resp = new DemoItemResp();
        resp.setId(entity.getId());
        resp.setName(entity.getName());
        resp.setStatus(entity.getStatus());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        return resp;
    }
}
