package com.minichat.friend.service.impl;

import com.alibaba.fastjson2.TypeReference;
import com.minichat.common.cache.CacheKeys;
import com.minichat.common.mq.MqConstants;
import com.minichat.common.core.exception.ErrorCode;
import com.minichat.common.core.exception.FriendException;
import com.minichat.common.cache.CacheClient;
import com.minichat.friend.dto.FriendGroupUpdateDTO;
import com.minichat.friend.dto.FriendRemarkUpdateDTO;
import com.minichat.friend.mapper.FriendMapper;
import com.minichat.friend.service.FriendService;
import com.minichat.friend.vo.FriendDetailVO;
import com.minichat.friend.vo.FriendGroupItemVO;
import com.minichat.friend.vo.FriendGroupVO;
import com.minichat.friend.vo.FriendVO;
import com.minichat.space.dto.SpacePostMqDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final FriendMapper friendMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheClient cacheClient;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public List<FriendVO> getFriendList(Long currentUserId) {
        List<FriendVO> friendVOList = cacheClient.queryWithPassThrough(CacheKeys.FRIEND_LIST_PREFIX, currentUserId,
                new TypeReference<List<FriendVO>>() {},
                friendMapper::selectFriendList,
                CacheKeys.EXPIRE_NORMAL + new Random().nextLong(10), TimeUnit.MINUTES);
        return friendVOList;
    }

    @Override
    public void updateFriendRemark(Long currentUserId, FriendRemarkUpdateDTO friendRemarkUpdateDTO) {
        if (friendRemarkUpdateDTO.getRemarkName() != null && friendRemarkUpdateDTO.getRemarkName().trim().isEmpty()) {
            friendRemarkUpdateDTO.setRemarkName(null);
        }
        friendMapper.updateFriendRemark(currentUserId, friendRemarkUpdateDTO);
        cacheClient.delete(CacheKeys.friendList(currentUserId));
        cacheClient.delete(CacheKeys.friendDetail(currentUserId, friendRemarkUpdateDTO.getFriendId()));
    }

    @Override
    public List<FriendGroupVO> getFriendGroupList(Long currentUserId) {
        List<FriendGroupVO> friendGroupVOList = cacheClient.queryWithPassThrough(CacheKeys.FRIEND_GROUP_PREFIX, currentUserId,
                new TypeReference<List<FriendGroupVO>>() {},
                friendMapper::selectFriendGroupList,
                CacheKeys.EXPIRE_NORMAL + new Random().nextLong(10), TimeUnit.MINUTES);
        return friendGroupVOList;
    }

    @Override
    public List<FriendGroupItemVO> getFriendGroupItemList(Long currentUserId, String groupName) {
        List<FriendGroupItemVO> friendGroupItemVOList = friendMapper.selectFriendGroupItemList(currentUserId, groupName);

        List<Long> friendIds = friendGroupItemVOList.stream()
                .map(FriendGroupItemVO::getFriendId)
                .toList();

        List<String> redisKeys = friendIds.stream()
                .map(CacheKeys::userOnline)
                .collect(Collectors.toList());

        List<Object> onlineStatusList = redisTemplate.opsForValue().multiGet(redisKeys);

        for (int i = 0; i < friendGroupItemVOList.size(); i++) {
            FriendGroupItemVO friendGroupItemVO = friendGroupItemVOList.get(i);
            Object status = onlineStatusList.get(i);
            friendGroupItemVO.setOnlineStatus(status != null);
        }

        return friendGroupItemVOList;
    }

    @Override
    public FriendDetailVO getFriendDetail(Long currentUserId, Long friendId) {
        FriendDetailVO friendDetailVO = cacheClient.queryWithPassThrough(CacheKeys.FRIEND_DETAIL_PREFIX, currentUserId + ":" + friendId,
                new TypeReference<FriendDetailVO>() {},
                id -> friendMapper.selectFriendDetail(currentUserId, Long.parseLong(id.split(":")[1])),
                CacheKeys.EXPIRE_NORMAL + new Random().nextLong(10), TimeUnit.MINUTES);
        return friendDetailVO;
    }

    @Override
    @Transactional
    public void deleteFriend(Long currentUserId, Long friendId) {
        if (currentUserId == null || friendId == null) {
            throw new FriendException(ErrorCode.BAD_REQUEST, "用户ID和好友ID不能为空");
        }

        int count1 = friendMapper.selectFriendByUserIdAndFriendId(currentUserId, friendId);
        int count2 = friendMapper.selectFriendByUserIdAndFriendId(friendId, currentUserId);
        if (count1 == 0 && count2 == 0) {
            throw new FriendException(ErrorCode.FRIEND_NOT_FOUND, "好友关系不存在");
        }

        LocalDateTime deletedTime = LocalDateTime.now();

        if (count1 > 0) {
            friendMapper.deleteFriend(currentUserId, friendId, deletedTime);
        }

        if (count2 > 0) {
            friendMapper.deleteFriend(friendId, currentUserId, deletedTime);
        }

        cacheClient.delete(CacheKeys.friendList(currentUserId));
        cacheClient.delete(CacheKeys.friendDetail(currentUserId, friendId));
        cacheClient.delete(CacheKeys.friendGroup(currentUserId));
        cacheClient.delete(CacheKeys.friendList(friendId));
        cacheClient.delete(CacheKeys.friendDetail(friendId, currentUserId));
        cacheClient.delete(CacheKeys.friendGroup(friendId));

        SpacePostMqDTO spacePostMqDTO1 = SpacePostMqDTO.builder()
                .authorId(friendId)
                .targetUserId(currentUserId)
                .build();
        rabbitTemplate.convertAndSend(MqConstants.SPACE_POST_EXCHANGE,
                MqConstants.SPACE_POST_DELETEALL_ROUTING_KEY,
                spacePostMqDTO1);
        SpacePostMqDTO spacePostMqDTO2 = SpacePostMqDTO.builder()
                .authorId(currentUserId)
                .targetUserId(friendId)
                .build();
        rabbitTemplate.convertAndSend(MqConstants.SPACE_POST_EXCHANGE,
                MqConstants.SPACE_POST_DELETEALL_ROUTING_KEY,
                spacePostMqDTO2);
    }

    @Override
    public void updateFriendGroup(Long currentUserId, FriendGroupUpdateDTO friendGroupUpdateDTO) {
        friendMapper.updateFriendGroup(currentUserId, friendGroupUpdateDTO);
        cacheClient.delete(CacheKeys.friendList(currentUserId));
        cacheClient.delete(CacheKeys.friendDetail(currentUserId, friendGroupUpdateDTO.getFriendId()));
        cacheClient.delete(CacheKeys.friendGroup(currentUserId));
    }
}
