package com.minichat.user.service;

import com.minichat.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAsyncServiceImpl implements LoginAsyncService {

    private final UserMapper userMapper;
    @Override
    @Async("loginAsyncExecutor") // 必须指定异步执行器，否则会使用默认的执行器
    public void asyncUpdateLastLoginTime(Long userId) {
        try{
            userMapper.updateLastLoginTime(userId);
            log.info("异步更新用户最后登录时间成功, userId: {}", userId);
        }catch (Exception e){
            log.error("异步更新用户最后登录时间失败", e);
        }
    }
}
