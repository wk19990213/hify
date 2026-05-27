package com.hify.chat.service;

/** 流式事件回调。onDelta 返回 false 表示客户端已断开，调用方应取消上游请求。 */
public interface StreamEventHandler {
    /** @return false if client disconnected, caller should cancel upstream */
    boolean onDelta(String delta);
    void onComplete();
    void onError(Throwable t);
}
