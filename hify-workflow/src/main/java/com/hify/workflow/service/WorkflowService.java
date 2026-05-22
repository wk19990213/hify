package com.hify.workflow.service;

import com.hify.common.result.PageResult;
import com.hify.workflow.dto.*;

public interface WorkflowService {
    Long create(WorkflowCreateReq req);
    void update(Long id, WorkflowUpdateReq req);
    void delete(Long id);
    PageResult<WorkflowResp> list(WorkflowListParams params);
    WorkflowResp getDetail(Long id);
    WorkflowInstanceResp run(Long id, WorkflowRunReq req);
    PageResult<WorkflowInstanceResp> listInstances(Long workflowId, Integer page, Integer pageSize);
    WorkflowInstanceResp getInstanceDetail(Long instanceId);
}
