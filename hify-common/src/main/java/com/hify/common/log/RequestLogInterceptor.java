package com.hify.common.log;

import cn.hutool.core.util.IdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class RequestLogInterceptor implements HandlerInterceptor {

    private static final String TRACE_ID = "traceId";
    private static final long SLOW_THRESHOLD_MS = 1000;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) {
        String traceId = IdUtil.fastSimpleUUID();
        MDC.put(TRACE_ID, traceId);
        request.setAttribute("_startTime", System.currentTimeMillis());
        request.setAttribute("_traceId", traceId);
        response.setHeader("X-TraceId", traceId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute("_startTime");
        long duration = startTime != null ? System.currentTimeMillis() - startTime : -1;
        String method = request.getMethod();
        String path = request.getRequestURI();
        int status = response.getStatus();

        if (duration > SLOW_THRESHOLD_MS) {
            log.warn("{} {} {} {}ms [SLOW]", method, path, status, duration);
        } else {
            log.info("{} {} {} {}ms", method, path, status, duration);
        }

        MDC.clear();
    }
}
