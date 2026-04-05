package com.minichat.user.controller;

import com.minichat.common.result.Result;
import com.minichat.user.service.UserOnlineStatusService;
import com.minichat.common.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/user-status")
@RequiredArgsConstructor
public class UserStatusController {

    private final UserOnlineStatusService userOnlineStatusService;

    //查询指定用户是否在线
    @GetMapping("/online/{userId}")
    public Result<Boolean> isUserOnline(@PathVariable Long userId) {
        Boolean isOnline = userOnlineStatusService.isUserOnline(userId);
        return Result.success(isOnline);
    }

    //查询当前用户是否在线
    @GetMapping("/online/me")
    public Result<Boolean> isUserOnlineMe() {
        Long currentUserId = UserContext.getCurUserId();
        Boolean isOnline = userOnlineStatusService.isUserOnline(currentUserId);
        return Result.success(isOnline);
    }

    //批量查询用户是否在线
    @PostMapping("/online/batch")
    public Result<Map<Long, Boolean>> batchCheckUserOnlineStatus(@RequestBody Set<Long> userIds) {
        Map<Long, Boolean> onlineStatusMap = new HashMap<>();
        for (Long userId : userIds) {
            onlineStatusMap.put(userId, userOnlineStatusService.isUserOnline(userId));
        }
        return Result.success(onlineStatusMap);
    }

    //查询所有在线用户
    @GetMapping("/online/all")
    public Result<Set<String>> getAllOnlineUsersId() {
        Set<String> onlineUsers = userOnlineStatusService.getAllOnlineUserIds();
        return Result.success(onlineUsers);
    }
}
