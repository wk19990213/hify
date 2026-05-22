package com.hify.workflow.engine;

public interface NodeExecutor {
    String getType();
    NodeExecResult execute(NodeExecContext context);
}
