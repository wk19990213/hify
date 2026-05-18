package com.hify.common.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    /**
     * LLM 调用线程池（阻塞等待完整响应）
     */
    @Bean
    @Qualifier("llmExecutor")
    public ThreadPoolExecutor llmExecutor() {
        return new ThreadPoolExecutor(
                10, 50,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                new ThreadFactoryBuilder().setNameFormat("llm-%d").setDaemon(true).build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 异步任务线程池（日志写入等非关键任务）
     */
    @Bean
    @Qualifier("asyncExecutor")
    public ThreadPoolExecutor asyncExecutor() {
        return new ThreadPoolExecutor(
                5, 20,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(200),
                new ThreadFactoryBuilder().setNameFormat("async-%d").setDaemon(true).build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
