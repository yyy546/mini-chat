package com.minichat.friend.service.impl;

import com.minichat.common.cache.CacheKeys;
import com.minichat.friend.constants.FriendConstants;
import com.minichat.common.core.constants.RequestConstants;
import com.minichat.common.core.exception.ErrorCode;
import com.minichat.common.core.exception.FriendException;
import com.minichat.common.cache.CacheClient;
import com.minichat.common.security.jwt.UserContext;
import com.minichat.friend.dto.FriendRequestDTO;
import com.minichat.friend.dto.HandleFriendRequestDTO;
import com.minichat.friend.entity.Friend;
import com.minichat.friend.entity.FriendRequest;
import com.minichat.friend.mapper.FriendMapper;
import com.minichat.friend.mapper.FriendRequestMapper;
import com.minichat.friend.service.FriendRequestService;
import com.minichat.friend.vo.FriendRequestVO;
import com.minichat.friend.vo.SentFriendRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
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
    public String sendFriendRequest(FriendRequestDTO friendRequestDTO) {
        if (UserContext.getCurUserId().equals(friendRequestDTO.getToUserId())) {
            throw new FriendException(ErrorCode.FRIEND_SELF_REQUEST, "不能申请自己为好友");
        }
        int count = friendMapper.selectFriendByUserIdAndFriendId(UserContext.getCurUserId(), friendRequestDTO.getToUserId())
                + friendMapper.selectFriendByUserIdAndFriendId(friendRequestDTO.getToUserId(), UserContext.getCurUserId());
        if (count != 0) {
            throw new FriendException(ErrorCode.FRIEND_ALREADY_EXISTS, "已添加为好友，无需重复申请");
        }
        FriendRequest request = friendRequestMapper.selectByFromUserIdAndToUserId(UserContext.getCurUserId(), friendRequestDTO.getToUserId());
        if (request != null && RequestConstants.PROCESSING.equals(request.getStatus())) {
            return "已发送好友申请，请等待对方处理";
        } else if (request != null && RequestConstants.REJECTED.equals(request.getStatus())) {

            request.setStatus(RequestConstants.PROCESSING);
            request.setMessage(friendRequestDTO.getMessage());
            friendRequestMapper.update(request);
            return "对方已拒绝好友申请，已重新发送申请";
        }

        FriendRequest friendRequest = new FriendRequest();
        BeanUtils.copyProperties(friendRequestDTO, friendRequest);
        friendRequest.setFromUserId(UserContext.getCurUserId());
        friendRequest.setStatus(RequestConstants.PROCESSING);

        friendRequestMapper.insert(friendRequest);
        return "好友申请已发送,等待对方处理";
    }

    @Override
    @Transactional
    public void handleFriendRequest(HandleFriendRequestDTO handleFriendRequestDTO) {
        FriendRequest friendRequest = friendRequestMapper.selectById(handleFriendRequestDTO.getRequestId());

        if (friendRequest == null) {
            throw new FriendException(ErrorCode.FRIEND_NOT_FOUND, "好友申请不存在");
        }

        friendRequest.setProcessedTime(LocalDateTime.now());

        if (!friendRequest.getToUserId().equals(UserContext.getCurUserId())) {
            throw new FriendException(ErrorCode.FORBIDDEN, "无权处理该好友申请");
        }

        if (RequestConstants.SUCCESS.equals(handleFriendRequestDTO.getStatus())) {
            LocalDateTime now = LocalDateTime.now();

            Friend existingFriend1 = friendMapper.selectFriendRecord(
                    friendRequest.getFromUserId(),
                    friendRequest.getToUserId()
            );

            if (existingFriend1 != null && existingFriend1.getIsDeleted() != null && FriendConstants.DELETED.equals(existingFriend1.getIsDeleted())) {
                Friend friend1 = Friend.builder()
                        .userId(friendRequest.getFromUserId())
                        .friendId(friendRequest.getToUserId())
                        .groupName("我的好友")
                        .isBlocked(FriendConstants.NOT_BLOCKED)
                        .createdTime(now)
                        .build();
                friendMapper.restoreFriend(friend1);
                cacheClient.delete(CacheKeys.friendList(friendRequest.getFromUserId()));
            } else if (existingFriend1 == null) {
                Friend friend1 = Friend.builder()
                        .userId(friendRequest.getFromUserId())
                        .friendId(friendRequest.getToUserId())
                        .groupName("我的好友")
                        .isBlocked(FriendConstants.NOT_BLOCKED)
                        .isDeleted(FriendConstants.NOT_DELETED)
                        .createdTime(now)
                        .build();
                friendMapper.insert(friend1);
                cacheClient.delete(CacheKeys.friendList(friendRequest.getFromUserId()));
            }

            Friend existingFriend2 = friendMapper.selectFriendRecord(
                    friendRequest.getToUserId(),
                    friendRequest.getFromUserId()
            );

            if (existingFriend2 != null && existingFriend2.getIsDeleted() != null && FriendConstants.DELETED.equals(existingFriend2.getIsDeleted())) {
                Friend friend2 = Friend.builder()
                        .userId(friendRequest.getToUserId())
                        .friendId(friendRequest.getFromUserId())
                        .groupName("我的好友")
                        .isBlocked(FriendConstants.NOT_BLOCKED)
                        .createdTime(now)
                        .build();
                friendMapper.restoreFriend(friend2);
                cacheClient.delete(CacheKeys.friendList(friendRequest.getToUserId()));

            } else if (existingFriend2 == null) {
                Friend friend2 = Friend.builder()
                        .userId(friendRequest.getToUserId())
                        .friendId(friendRequest.getFromUserId())
                        .groupName("我的好友")
                        .isBlocked(FriendConstants.NOT_BLOCKED)
                        .isDeleted(FriendConstants.NOT_DELETED)
                        .createdTime(now)
                        .build();
                friendMapper.insert(friend2);
                cacheClient.delete(CacheKeys.friendList(friendRequest.getToUserId()));

            }

            friendRequestMapper.delete(friendRequest.getId());
        } else {
            friendRequest.setStatus(handleFriendRequestDTO.getStatus());
            friendRequestMapper.update(friendRequest);
        }
    }

    @Override
    public List<FriendRequestVO> getFriendRequestList(Long currentUserId) {
        if (currentUserId == null) {
            throw new FriendException(ErrorCode.BAD_REQUEST, "当前用户未登录");
        }
        return friendRequestMapper.selectReceivedRequests(currentUserId);
    }

    @Override
    public List<SentFriendRequestVO> getSentFriendRequestList(Long currentUserId) {
        if (currentUserId == null) {
            throw new FriendException(ErrorCode.BAD_REQUEST, "当前用户未登录");
        }
        return friendRequestMapper.selectSentRequests(currentUserId);
    }

}
