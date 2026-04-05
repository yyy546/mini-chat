package com.minichat.friend.service.impl;

import com.minichat.common.constants.RedisConstants;
import com.minichat.common.util.CacheClient;
import com.minichat.friend.dto.FriendRequestDTO;
import com.minichat.friend.dto.HandleFriendRequestDTO;
import com.minichat.friend.vo.FriendRequestVO;
import com.minichat.friend.vo.SentFriendRequestVO;
import com.minichat.common.constants.FriendConstants;
import com.minichat.common.constants.RequestConstants;
import com.minichat.friend.entity.Friend;
import com.minichat.friend.entity.FriendRequest;
import com.minichat.friend.mapper.FriendMapper;
import com.minichat.friend.mapper.FriendRequestMapper;
import com.minichat.common.result.Result;
import com.minichat.friend.service.FriendRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import com.minichat.common.util.UserContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendRequestServiceImpl implements FriendRequestService {

    private final FriendMapper friendMapper;
    private final FriendRequestMapper friendRequestMapper;
    private final CacheClient cacheClient;

    @Override
    public Result<String> sendFriendRequest(FriendRequestDTO friendRequestDTO) {
        // 检查是否自己申请
        if (UserContext.getCurUserId().equals(friendRequestDTO.getToUserId())) {
            return Result.error("不能申请自己为好友");
        }
        // 检查是否已添加为好友
        int count = friendMapper.selectFriendByUserIdAndFriendId(UserContext.getCurUserId(), friendRequestDTO.getToUserId())
                + friendMapper.selectFriendByUserIdAndFriendId(friendRequestDTO.getToUserId(), UserContext.getCurUserId());
        if(count != 0){
            return Result.error("已添加为好友，无需重复申请");
        }
        // 检查是否存在重复申请
        FriendRequest request = friendRequestMapper.selectByFromUserIdAndToUserId(UserContext.getCurUserId(), friendRequestDTO.getToUserId());
        if(request != null && RequestConstants.PROCESSING.equals(request.getStatus())){
            return Result.success("已发送好友申请，请等待对方处理");
        } else if (request != null && RequestConstants.REJECTED.equals(request.getStatus())) {

            request.setStatus(RequestConstants.PROCESSING);
            request.setMessage(friendRequestDTO.getMessage());
            friendRequestMapper.update(request);
            return Result.success("对方已拒绝好友申请，已重新发送申请");
        }

        FriendRequest friendRequest = new FriendRequest();
        BeanUtils.copyProperties(friendRequestDTO, friendRequest);
        friendRequest.setFromUserId(UserContext.getCurUserId());
        friendRequest.setStatus(RequestConstants.PROCESSING);

        friendRequestMapper.insert(friendRequest);
        return Result.success("好友申请已发送,等待对方处理");
    }

    @Override
    @Transactional
    public Result<String> handleFriendRequest(HandleFriendRequestDTO handleFriendRequestDTO) {
        // 检查好友申请是否存在
        FriendRequest friendRequest = friendRequestMapper.selectById(handleFriendRequestDTO.getRequestId());

        if (friendRequest == null) {
            return Result.error("好友申请不存在");
        }

        friendRequest.setProcessedTime(LocalDateTime.now());

        // 检查权限：只有接收方才能处理好友申请
        if (!friendRequest.getToUserId().equals(UserContext.getCurUserId())) {
            return Result.error("无权处理该好友申请");
        }

        // 处理好友申请
        if (RequestConstants.SUCCESS.equals(handleFriendRequestDTO.getStatus())) {
            // 同意好友申请
            LocalDateTime now = LocalDateTime.now();

            // 检查并处理第一条好友关系
            Friend existingFriend1 = friendMapper.selectFriendRecord(
                    friendRequest.getFromUserId(),
                    friendRequest.getToUserId()
            );

            if (existingFriend1 != null && existingFriend1.getIsDeleted() != null && FriendConstants.DELETED.equals(existingFriend1.getIsDeleted())) {
                // 恢复已删除的好友关系
                Friend friend1 = Friend.builder()
                        .userId(friendRequest.getFromUserId())
                        .friendId(friendRequest.getToUserId())
                        .groupName("我的好友")
                        .isBlocked(FriendConstants.NOT_BLOCKED)
                        .createdTime(now)
                        .build();
                friendMapper.restoreFriend(friend1);
                //删除用户fromUserId的好友列表缓存
                cacheClient.delete(RedisConstants.CACHE_FRIEND_LIST_KEY_PREFIX + friendRequest.getFromUserId());
            } else if (existingFriend1 == null) {
                // 插入新记录
                Friend friend1 = Friend.builder()
                        .userId(friendRequest.getFromUserId())
                        .friendId(friendRequest.getToUserId())
                        .groupName("我的好友")
                        .isBlocked(FriendConstants.NOT_BLOCKED)
                        .isDeleted(FriendConstants.NOT_DELETED)
                        .createdTime(now)
                        .build();
                friendMapper.insert(friend1);
                //删除用户fromUserId的好友列表缓存
                cacheClient.delete(RedisConstants.CACHE_FRIEND_LIST_KEY_PREFIX + friendRequest.getFromUserId());
            } else {
                // 记录已存在且未删除，说明可能已经添加过了，直接跳过
                // 这种情况理论上不应该发生（因为sendFriendRequest已经检查），但作为防御性编程
            }

            // 检查并处理第二条好友关系（反向关系）
            Friend existingFriend2 = friendMapper.selectFriendRecord(
                    friendRequest.getToUserId(),
                    friendRequest.getFromUserId()
            );

            if (existingFriend2 != null && existingFriend2.getIsDeleted() != null && FriendConstants.DELETED.equals(existingFriend2.getIsDeleted())) {
                // 恢复已删除的好友关系
                Friend friend2 = Friend.builder()
                        .userId(friendRequest.getToUserId())
                        .friendId(friendRequest.getFromUserId())
                        .groupName("我的好友")
                        .isBlocked(FriendConstants.NOT_BLOCKED)
                        .createdTime(now)
                        .build();
                friendMapper.restoreFriend(friend2);
                //删除用户toUserId的好友列表缓存
                cacheClient.delete(RedisConstants.CACHE_FRIEND_LIST_KEY_PREFIX + friendRequest.getToUserId());

            } else if (existingFriend2 == null) {
                // 插入新记录
                Friend friend2 = Friend.builder()
                        .userId(friendRequest.getToUserId())
                        .friendId(friendRequest.getFromUserId())
                        .groupName("我的好友")
                        .isBlocked(FriendConstants.NOT_BLOCKED)
                        .isDeleted(FriendConstants.NOT_DELETED)
                        .createdTime(now)
                        .build();
                friendMapper.insert(friend2);
                //删除用户toUserId的好友列表缓存
                cacheClient.delete(RedisConstants.CACHE_FRIEND_LIST_KEY_PREFIX + friendRequest.getToUserId());

            } else {
                // 记录已存在且未删除，说明可能已经添加过了，直接跳过
                // 这种情况理论上不应该发生（因为sendFriendRequest已经检查），但作为防御性编程
            }

            // 同意好友申请后，删除好友申请记录
            friendRequestMapper.delete(friendRequest.getId());

            return Result.success("好友申请已同意");
        }else{
            //更新好友申请状态
            friendRequest.setStatus(handleFriendRequestDTO.getStatus());
            friendRequestMapper.update(friendRequest);
            return Result.success("好友申请已拒绝");
        }
    }

    @Override
    public List<FriendRequestVO> getFriendRequestList(Long currentUserId) {
        if(currentUserId == null){
            throw new IllegalArgumentException("当前用户未登录");
        }
        return friendRequestMapper.selectReceivedRequests(currentUserId);
    }

    @Override
    public List<SentFriendRequestVO> getSentFriendRequestList(Long currentUserId) {
        if(currentUserId == null){
            throw new IllegalArgumentException("当前用户未登录");
        }
        return friendRequestMapper.selectSentRequests(currentUserId);
    }

}