package com.hify.common.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hify.common.entity.BaseEntity;
import com.hify.common.exception.BizException;
import com.hify.common.result.PageResult;
import com.hify.common.util.PageHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 通用 Service 基类 — 提供标准 CRUD 模板方法。
 * 子类只需注入对应 Mapper 即可获得完整 CRUD 能力。
 *
 * @param <M> MyBatis-Plus Mapper
 * @param <E> 实体类（需继承 BaseEntity）
 * @param <R> 请求 DTO
 * @param <P> 响应 DTO
 */
public abstract class BaseServiceImpl<M extends BaseMapper<E>, E extends BaseEntity, R, P> {

    protected abstract M getMapper();

    protected abstract P toResp(E entity);

    protected E toEntity(R req) {
        E entity = createEntity();
        BeanUtils.copyProperties(req, entity);
        return entity;
    }

    protected abstract E createEntity();

    protected LambdaQueryWrapper<E> buildListWrapper() {
        return new LambdaQueryWrapper<E>().eq(BaseEntity::getDeleted, 0);
    }

    @Transactional
    public Long create(R req) {
        E entity = toEntity(req);
        entity.setId(null);
        getMapper().insert(entity);
        return entity.getId();
    }

    @Transactional
    public void update(Long id, R req) {
        E entity = getMapper().selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw BizException.notFound("记录不存在");
        }
        BeanUtils.copyProperties(req, entity, "id");
        getMapper().updateById(entity);
    }

    @Transactional
    public void delete(Long id) {
        E entity = getMapper().selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw BizException.notFound("记录不存在");
        }
        getMapper().deleteById(id);
    }

    public PageResult<P> list(Integer page, Integer pageSize) {
        var pageParam = PageHelper.<E>toPage(page, pageSize);
        var wrapper = buildListWrapper().orderByDesc(BaseEntity::getCreatedAt);
        var pageResult = getMapper().selectPage(pageParam, wrapper);
        List<P> list = pageResult.getRecords().stream().map(this::toResp).toList();
        return PageResult.ok(list, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize());
    }

    public P getDetail(Long id) {
        E entity = getMapper().selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw BizException.notFound("记录不存在");
        }
        return toResp(entity);
    }
}
