package com.minichat.group.service.impl;

import com.minichat.common.constants.RedisConstants;
import com.minichat.common.util.CacheClient;
import com.minichat.group.mapper.ChatGroupMapper;
import com.minichat.group.mapper.GroupMemberMapper;
import jakarta.annotation.Resource;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractGroupService {

    @Resource
    protected  GroupMemberMapper groupMemberMapper;
    @Resource
    protected  ChatGroupMapper chatGroupMapper;
    @Resource
    protected  CacheClient cacheClient;

    protected void updateMemberCountAndRefreshCache(Long groupId){
        // 更新群成员数量
        int memberCount = groupMemberMapper.selectMemberIdsByGroupId(groupId).size();
        chatGroupMapper.updateMemberCount(groupId, memberCount);
        // 删除所有用户的群组缓存
        refreshGroupMemberCache(groupId);
    }

    /**
     * 刷新群组成员缓存
     * @param groupId 群组ID
     */
    protected void refreshGroupMemberCache(Long groupId) {
        List<Long> memberIds = groupMemberMapper.selectMemberIdsByGroupId(groupId);
        List<String> keys = new ArrayList<>();
        for (Long memberId : memberIds) {
            keys.add(RedisConstants.CACHE_USER_GROUP_LIST_KEY_PREFIX + memberId);
        }
        cacheClient.deleteBatch(keys);
    }
}
