package com.hify.mcp.service;

import com.hify.common.result.PageResult;
import com.hify.mcp.dto.*;

import java.util.List;

public interface McpService {
    Long create(McpServerCreateReq req);
    void update(Long id, McpServerUpdateReq req);
    void delete(Long id);
    PageResult<McpServerResp> list(McpServerListParams params);
    McpServerResp getDetail(Long id);
    List<McpServerToolsResp> getAllTools();
}
