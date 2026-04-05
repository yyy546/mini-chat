package com.minichat.session.service;

import com.minichat.session.vo.SessionVO;

import java.util.List;

public interface SessionService {

    /**
     * 获取会话列表
     * @param currentUserId 当前用户ID
     * @return 会话列表
     */
    List<SessionVO> getSessionList(Long currentUserId);
}
