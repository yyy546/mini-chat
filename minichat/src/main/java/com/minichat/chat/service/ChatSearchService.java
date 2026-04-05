package com.minichat.chat.service;

import com.minichat.chat.entity.EsChatMessage;

import java.util.List;

public interface ChatSearchService {

    /**
     * 初始化es索引
     */
    void initIndex();

    /**
     * 搜索聊天记录
     * @param keyword 搜索关键词
     * @param type 搜索类型 0-单聊 1-群聊
     * @param targetId 目标id 单聊-对方id 群聊-群id
     * @param currentUserId 当前用户id
     * @return 聊天记录列表
     */
    List<EsChatMessage> search(String keyword, Integer type, Long targetId, Long currentUserId);

    /**
     * 保存聊天记录
     * @param message 聊天记录
     */
    void saveChatMessage(EsChatMessage message);

    /**
     * 删除聊天记录
     * @param id 聊天记录id
     */
    void deleteChatMessage(String id);
}
