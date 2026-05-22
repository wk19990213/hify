package com.hify.workflow.engine;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NodeExecResult {
    private boolean success;
    private Object output;
    private String errorMsg;
}
