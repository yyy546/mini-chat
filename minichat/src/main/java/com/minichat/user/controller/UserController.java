package com.minichat.user.controller;

import com.minichat.user.dto.UserUpdateDTO;
import com.minichat.user.vo.UserDetailVO;
import com.minichat.user.vo.UserSearchVO;
import com.minichat.common.result.Result;
import com.minichat.user.service.UserService;
import com.minichat.common.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 获取当前登录用户的详细信息
    @GetMapping("/me")
    public Result<UserDetailVO> getCurrentUserDetail(){
        Long currentUserId = UserContext.getCurUserId();
        UserDetailVO userDetailVO = userService.getUserDetail(currentUserId);
        return Result.success(userDetailVO);
    }

    // 更新当前登录用户的头像
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestPart("avatar") MultipartFile avatar){
        Long currentUserId = UserContext.getCurUserId();

        // 上传头像
        String avatarUrl = userService.uploadAvatar(currentUserId, avatar);
        return Result.success(avatarUrl);
    }

    // 更新当前登录用户的详细信息
    @PutMapping("/me")
    public Result<UserDetailVO> updateCurrentUserDetail(@Valid @RequestBody UserUpdateDTO userUpdateDTO){
        Long currentUserId = UserContext.getCurUserId();
        UserDetailVO userDetailVO = userService.updateUserDetail(currentUserId, userUpdateDTO);
        return Result.success(userDetailVO);
    }

    // 搜索用户
    @GetMapping("/search")
    public Result<List<UserSearchVO>> searchUsers(@RequestParam String keyword){
        Long currentUserId = UserContext.getCurUserId();
        List<UserSearchVO> userSearchVOList = userService.searchUsers(keyword, currentUserId);
        return Result.success(userSearchVOList);
    }

}
