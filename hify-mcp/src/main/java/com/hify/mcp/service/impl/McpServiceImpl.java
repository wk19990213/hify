package com.hify.mcp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hify.common.exception.BizException;
import com.hify.common.result.PageResult;
import com.hify.common.util.PageHelper;
import com.hify.mcp.dto.*;
import com.hify.mcp.entity.McpServerEntity;
import com.hify.mcp.mapper.McpServerMapper;
import com.hify.mcp.mcp.McpClientManager;
import com.hify.mcp.service.McpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpServiceImpl implements McpService {

    private final McpServerMapper serverMapper;
    private final McpClientManager clientManager;

    @Override
    @Transactional
    public Long create(McpServerCreateReq req) {
        McpServerEntity entity = new McpServerEntity();
        BeanUtils.copyProperties(req, entity);
        serverMapper.insert(entity);
        log.info("MCP server created: id={}, name={}", entity.getId(), entity.getName());
        return entity.getId();
    }

    @Override
    @Transactional
    public void update(Long id, McpServerUpdateReq req) {
        McpServerEntity entity = serverMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw BizException.notFound("MCP 服务器不存在");
        }
        if (req.getName() != null) entity.setName(req.getName());
        if (req.getUrl() != null) entity.setUrl(req.getUrl());
        if (req.getAuthConfig() != null) entity.setAuthConfig(req.getAuthConfig());
        if (req.getTransportType() != null) entity.setTransportType(req.getTransportType());
        if (req.getStatus() != null) entity.setStatus(req.getStatus());
        serverMapper.updateById(entity);
        clientManager.evict(id);
        log.info("MCP server updated: id={}, name={}", id, entity.getName());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        McpServerEntity entity = serverMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw BizException.notFound("MCP 服务器不存在");
        }
        serverMapper.deleteById(id);
        clientManager.evict(id);
        log.info("MCP server deleted: id={}", id);
    }

    @Override
    public PageResult<McpServerResp> list(McpServerListParams params) {
        Page<McpServerEntity> page = PageHelper.toPage(params.getPage(), params.getPageSize());
        var wrapper = new LambdaQueryWrapper<McpServerEntity>()
                .eq(McpServerEntity::getDeleted, 0)
                .eq(params.getStatus() != null, McpServerEntity::getStatus, params.getStatus())
                .like(params.getName() != null && !params.getName().isBlank(),
                        McpServerEntity::getName, params.getName())
                .orderByDesc(McpServerEntity::getCreatedAt);
        var pageResult = serverMapper.selectPage(page, wrapper);
        List<McpServerResp> list = pageResult.getRecords().stream()
                .map(this::toResp)
                .toList();
        return PageResult.ok(list, pageResult.getTotal(), pageResult.getCurrent(),
                pageResult.getSize());
    }

    @Override
    public McpServerResp getDetail(Long id) {
        McpServerEntity entity = serverMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw BizException.notFound("MCP 服务器不存在");
        }
        return toResp(entity);
    }

    @Override
    public List<McpServerToolsResp> getAllTools() {
        var wrapper = new LambdaQueryWrapper<McpServerEntity>()
                .eq(McpServerEntity::getDeleted, 0)
                .eq(McpServerEntity::getStatus, 1);
        List<McpServerEntity> servers = serverMapper.selectList(wrapper);
        List<McpServerToolsResp> result = new ArrayList<>();
        for (McpServerEntity s : servers) {
            McpServerToolsResp resp = new McpServerToolsResp();
            resp.setServerId(s.getId());
            resp.setServerName(s.getName());
            resp.setTools(clientManager.listTools(s.getId()));
            result.add(resp);
        }
        return result;
    }

    private McpServerResp toResp(McpServerEntity entity) {
        McpServerResp resp = new McpServerResp();
        BeanUtils.copyProperties(entity, resp);
        return resp;
    }
}
