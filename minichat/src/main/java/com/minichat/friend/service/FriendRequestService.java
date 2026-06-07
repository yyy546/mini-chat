package com.minichat.friend.service;

import com.minichat.friend.dto.FriendRequestDTO;
import com.minichat.friend.dto.HandleFriendRequestDTO;
import com.minichat.friend.vo.FriendRequestVO;
import com.minichat.friend.vo.SentFriendRequestVO;

import java.util.List;

public interface FriendRequestService {
    String sendFriendRequest(FriendRequestDTO friendRequestDTO);

    void handleFriendRequest(HandleFriendRequestDTO handleFriendRequestDTO);

    List<FriendRequestVO> getFriendRequestList(Long currentUserId);

    List<SentFriendRequestVO> getSentFriendRequestList(Long currentUserId);
}
