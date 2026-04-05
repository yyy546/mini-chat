package com.minichat.friend.service.impl;

import com.alibaba.fastjson2.TypeReference;
import com.minichat.common.constants.MqConstants;
import com.minichat.common.constants.RedisConstants;
import com.minichat.common.util.CacheClient;
import com.minichat.friend.dto.FriendGroupUpdateDTO;
import com.minichat.friend.dto.FriendRemarkUpdateDTO;
import com.minichat.friend.vo.FriendDetailVO;
import com.minichat.friend.vo.FriendGroupItemVO;
import com.minichat.friend.vo.FriendGroupVO;
import com.minichat.friend.vo.FriendVO;
import com.minichat.friend.mapper.FriendMapper;
import com.minichat.friend.service.FriendService;
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
        List<FriendVO> friendVOList = cacheClient.queryWithPassThrough(RedisConstants.CACHE_FRIEND_LIST_KEY_PREFIX, currentUserId,
                new TypeReference<List<FriendVO>>() {},
                friendMapper::selectFriendList,
                RedisConstants.CACHE_NORMAL_EXPIRE_TIME + new Random().nextLong(10), TimeUnit.MINUTES);
        return friendVOList;
    }

    @Override
    public void updateFriendRemark(Long currentUserId, FriendRemarkUpdateDTO friendRemarkUpdateDTO) {
        if(friendRemarkUpdateDTO.getRemarkName() != null && friendRemarkUpdateDTO.getRemarkName().trim().isEmpty()){
            friendRemarkUpdateDTO.setRemarkName(null);
        }
        friendMapper.updateFriendRemark(currentUserId, friendRemarkUpdateDTO);
        // Cache-Aside: 删除缓存
        cacheClient.delete(RedisConstants.CACHE_FRIEND_LIST_KEY_PREFIX + currentUserId);
        cacheClient.delete(RedisConstants.CACHE_FRIEND_DETAIL_KEY_PREFIX + currentUserId + ":" + friendRemarkUpdateDTO.getFriendId());
    }

    @Override
    public List<FriendGroupVO> getFriendGroupList(Long currentUserId) {
        List<FriendGroupVO> friendGroupVOList = cacheClient.queryWithPassThrough(RedisConstants.CACHE_FRIEND_GROUP_KEY_PREFIX, currentUserId,
                new TypeReference<List<FriendGroupVO>>() {},
                friendMapper::selectFriendGroupList,
                RedisConstants.CACHE_NORMAL_EXPIRE_TIME + new Random().nextLong(10), TimeUnit.MINUTES);
        return friendGroupVOList;
    }

    @Override
    public List<FriendGroupItemVO> getFriendGroupItemList(Long currentUserId, String groupName) {
//        //获取好友分组下的好友列表
        List<FriendGroupItemVO> friendGroupItemVOList = friendMapper.selectFriendGroupItemList(currentUserId, groupName);

        //获取好友分组下的好友id列表
        List<Long> friendIds = friendGroupItemVOList.stream()
                .map(FriendGroupItemVO::getFriendId)
                .toList();

        //根据好友id列表构建redis key列表
        List<String> redisKeys = friendIds.stream()
                .map(id -> "user:online:" + id)
                .collect(Collectors.toList());

        //从redis中批量获取好友的在线状态
        List<Object> onlineStatusList = redisTemplate.opsForValue().multiGet(redisKeys);

        //将获取到的在线状态设置到好友分组下的好友列表中
        for(int i = 0; i < friendGroupItemVOList.size(); i++){
            FriendGroupItemVO friendGroupItemVO = friendGroupItemVOList.get(i);
            Object status = onlineStatusList.get(i);
            friendGroupItemVO.setOnlineStatus(status != null);
        }

        return friendGroupItemVOList;
    }

    @Override
    public FriendDetailVO getFriendDetail(Long currentUserId, Long friendId) {
        FriendDetailVO friendDetailVO = cacheClient.queryWithPassThrough(RedisConstants.CACHE_FRIEND_DETAIL_KEY_PREFIX, currentUserId + ":" + friendId,
                new TypeReference<FriendDetailVO>() {},
                id -> friendMapper.selectFriendDetail(currentUserId, Long.parseLong(id.split(":")[1])),
                RedisConstants.CACHE_NORMAL_EXPIRE_TIME + new Random().nextLong(10), TimeUnit.MINUTES);
        return friendDetailVO;
    }

    @Override
    @Transactional
    public void deleteFriend(Long currentUserId, Long friendId) {
        // 校验参数
        if (currentUserId == null || friendId == null) {
            throw new IllegalArgumentException("用户ID和好友ID不能为空");
        }
        
        // 检查好友关系是否存在（双向检查）
        int count1 = friendMapper.selectFriendByUserIdAndFriendId(currentUserId, friendId);
        int count2 = friendMapper.selectFriendByUserIdAndFriendId(friendId, currentUserId);
        if (count1 == 0 && count2 == 0) {
            throw new IllegalArgumentException("好友关系不存在");
        }
        
        LocalDateTime deletedTime = LocalDateTime.now();
        
        // 删除当前用户的好友关系（如果存在）
        if (count1 > 0) {
            friendMapper.deleteFriend(currentUserId, friendId, deletedTime);
        }
        
        // 删除对方的好友关系（双向删除，如果存在）
        if (count2 > 0) {
            friendMapper.deleteFriend(friendId, currentUserId, deletedTime);
        }

        // Cache-Aside: 删除缓存(双向删除)
        // 删除当前用户的好友列表缓存
        cacheClient.delete(RedisConstants.CACHE_FRIEND_LIST_KEY_PREFIX + currentUserId);
        cacheClient.delete(RedisConstants.CACHE_FRIEND_DETAIL_KEY_PREFIX + currentUserId + ":" + friendId);
        cacheClient.delete(RedisConstants.CACHE_FRIEND_GROUP_KEY_PREFIX + currentUserId);
        // 删除好友的好友列表缓存
        cacheClient.delete(RedisConstants.CACHE_FRIEND_LIST_KEY_PREFIX + friendId);
        cacheClient.delete(RedisConstants.CACHE_FRIEND_DETAIL_KEY_PREFIX + friendId + ":" + currentUserId);
        cacheClient.delete(RedisConstants.CACHE_FRIEND_GROUP_KEY_PREFIX + friendId);
        /*
        * 异步删除朋友圈帖子
        * */
        SpacePostMqDTO spacePostMqDTO1 = SpacePostMqDTO.builder()
                .authorId(friendId)
                .targetUserId(currentUserId)
                .build();
        // 发送到MQ
        rabbitTemplate.convertAndSend(MqConstants.SPACE_POST_EXCHANGE,
                MqConstants.SPACE_POST_DELETEALL_ROUTING_KEY,
                spacePostMqDTO1);
        SpacePostMqDTO spacePostMqDTO2 = SpacePostMqDTO.builder()
                .authorId(currentUserId)
                .targetUserId(friendId)
                .build();
        // 发送到MQ
        rabbitTemplate.convertAndSend(MqConstants.SPACE_POST_EXCHANGE,
                MqConstants.SPACE_POST_DELETEALL_ROUTING_KEY,
                spacePostMqDTO2);
    }

    @Override
    public void updateFriendGroup(Long currentUserId, FriendGroupUpdateDTO friendGroupUpdateDTO) {
        friendMapper.updateFriendGroup(currentUserId, friendGroupUpdateDTO);
        // Cache-Aside: 删除缓存
        cacheClient.delete(RedisConstants.CACHE_FRIEND_LIST_KEY_PREFIX + currentUserId);
        cacheClient.delete(RedisConstants.CACHE_FRIEND_DETAIL_KEY_PREFIX + currentUserId + ":" + friendGroupUpdateDTO.getFriendId());
        cacheClient.delete(RedisConstants.CACHE_FRIEND_GROUP_KEY_PREFIX + currentUserId);
    }
}
