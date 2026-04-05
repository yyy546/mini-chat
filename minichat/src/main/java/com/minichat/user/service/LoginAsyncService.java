package com.minichat.user.service;

public interface LoginAsyncService {

     /**
      * 异步更新用户最后登录时间
      */
    void asyncUpdateLastLoginTime(Long userId);
}
