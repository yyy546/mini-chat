package com.minichat.friend.service;

import com.minichat.friend.dto.FriendRequestDTO;
import com.minichat.friend.dto.HandleFriendRequestDTO;
import com.minichat.friend.vo.FriendRequestVO;
import com.minichat.friend.vo.SentFriendRequestVO;
import com.minichat.common.result.Result;

import java.util.List;

public interface FriendRequestService {
    /**
     * 发送好友申请
     */
    Result<String> sendFriendRequest(FriendRequestDTO friendRequestDTO);

    /**
     * 处理好友申请
     */
    Result<String> handleFriendRequest(HandleFriendRequestDTO handleFriendRequestDTO);

    /**
     * 获取好友申请列表
     */
    List<FriendRequestVO> getFriendRequestList(Long currentUserId);

    /**
     * 获取已发送好友申请列表
     */
    List<SentFriendRequestVO> getSentFriendRequestList(Long currentUserId);
}
