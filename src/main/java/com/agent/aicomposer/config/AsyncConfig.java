package com.agent.aicomposer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "articleExecutor")
    public Executor articleExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setKeepAliveSeconds(5);
        executor.setQueueCapacity(100);
        // 设置线程名称前缀
        executor.setThreadNamePrefix("article-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 线程池shutdown()后等待所有运行任务完成的时间
        // 这里指主线程的阻塞等待状态，而不是说60秒完了线程池会被强制关闭
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        return executor;
    }
}
