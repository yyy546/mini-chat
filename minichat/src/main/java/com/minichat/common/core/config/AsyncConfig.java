package com.minichat.common.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync // 必须加这个注解，开启异步功能
public class AsyncConfig {

    /**
     * 自定义异步线程池（用于登录相关的异步操作）
     */
    @Bean("loginAsyncExecutor") // 给线程池命名，方便指定使用
    public Executor loginAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数（根据服务器配置调整，开发环境2-4即可）
        executor.setCorePoolSize(4);
        // 最大线程数
        executor.setMaxPoolSize(8);
        // 队列容量（核心线程满了后，任务先入队列）
        executor.setQueueCapacity(100);
        // 线程名称前缀（方便日志排查）
        executor.setThreadNamePrefix("login-async-");
        // 拒绝策略：队列满+最大线程数满时，用调用者线程执行（避免任务丢失）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 线程空闲超时时间（非核心线程）
        executor.setKeepAliveSeconds(60);
        // 初始化线程池
        executor.initialize();
        return executor;
    }
}