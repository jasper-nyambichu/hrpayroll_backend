// config/AsyncConfig.java
package com.company.hrpayroll.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        // Every @Async method (notification dispatch, future biometric sync
        // fan-out) runs here — one virtual thread per task, no fixed pool
        // size to tune, since this is pure I/O-bound work.
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}