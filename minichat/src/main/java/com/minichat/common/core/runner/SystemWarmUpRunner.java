package com.minichat.common.core.runner;

import com.minichat.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemWarmUpRunner implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public void run(String... args) {
        log.info("系统预热开始...");

        // 1. 预热 BCryptPasswordEncoder (触发 SecureRandom 初始化)
        passwordEncoder.encode("warmup");

        // 2. 预热数据库连接 (执行一次查询以建立连接)

        userMapper.warmUp();

        log.info("系统预热完成.");
    }
}