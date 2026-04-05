package com.minichat.user.service;

import java.util.Set;

public interface UserOnlineStatusService {

    /**
     * 设置用户为在线状态
     */
    void setUserOnline(Long userId);

    /**
     * 判断用户是否在线
     */
    Boolean isUserOnline(Long userId);

    /**
     * 获取所有在线用户的ID
     */
    Set<String> getAllOnlineUserIds();

    /**
     * 设置用户为离线状态
     */
    void setUserOffline(Long userId);

    /**
     * 刷新用户在线状态
     */
    void renewUserOnlineStatus(Long userId);
}
