package com.minichat.common.runner;

import com.minichat.chat.service.ChatSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EsIndexInitializer implements CommandLineRunner {

    private static final int maxRetries = 5;

    private final ChatSearchService chatSearchService;

    @Async
    @Override
    public void run(String... args) throws Exception {
        log.info("开始异步初始化es索引");

        int retryCount = 0;

        while(retryCount < maxRetries){
            try{
                chatSearchService.initIndex();
                return;
            }catch (Exception e){
                log.error("初始化es索引失败，重试次数{}", retryCount, e);
                retryCount++;
                try{
                    Thread.sleep(5000);
                }catch (InterruptedException ex){
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
