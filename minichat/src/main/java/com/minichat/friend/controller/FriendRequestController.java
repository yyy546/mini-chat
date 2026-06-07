package com.minichat.friend.controller;

import com.minichat.friend.dto.FriendRequestDTO;
import com.minichat.friend.dto.HandleFriendRequestDTO;
import com.minichat.friend.vo.FriendRequestVO;
import com.minichat.friend.vo.SentFriendRequestVO;
import com.minichat.common.result.Result;
import com.minichat.friend.service.FriendRequestService;
import com.minichat.common.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/friend/request")
@RequiredArgsConstructor
public class FriendRequestController {

    private final FriendRequestService friendRequestService;

    @PostMapping("/send")
    public Result<String> sendFriendRequest(@Valid @RequestBody FriendRequestDTO friendRequestDTO) {
        Long currentUserId = UserContext.getCurUserId();
        if(currentUserId == null){
            return Result.error("用户未登录");
        }
        String msg = friendRequestService.sendFriendRequest(friendRequestDTO);
        return Result.success(msg);
    }

    @GetMapping("/sent")
    public Result<List<SentFriendRequestVO>> getSentFriendRequests() {
        Long currentUserId = UserContext.getCurUserId();
        List<SentFriendRequestVO> sentFriendRequestVOList = friendRequestService.getSentFriendRequestList(currentUserId);
        return Result.success(sentFriendRequestVOList);
    }

    @PostMapping("/handle")
    public Result<String> handleFriendRequest(@Valid @RequestBody HandleFriendRequestDTO handleFriendRequestDTO) {
        Long currentUserId = UserContext.getCurUserId();
        if(currentUserId == null){
            return Result.error("用户未登录");
        }
        friendRequestService.handleFriendRequest(handleFriendRequestDTO);
        return Result.success("处理成功");
    }

    @GetMapping("/list")
    public Result<List<FriendRequestVO>> getFriendRequestList() {
        Long currentUserId = UserContext.getCurUserId();
        List<FriendRequestVO> friendRequestVOList = friendRequestService.getFriendRequestList(currentUserId);
        return Result.success(friendRequestVOList);
    }
}
