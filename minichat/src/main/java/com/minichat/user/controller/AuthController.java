package com.minichat.user.controller;

import com.minichat.user.vo.UserLoginVO;
import com.minichat.user.dto.LoginDTO;
import com.minichat.user.dto.RegisterDTO;
import com.minichat.common.result.Result;
import com.minichat.user.service.UserOnlineStatusService;
import com.minichat.user.service.UserService;
import com.minichat.common.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserOnlineStatusService userOnlineStatusService;

    //用户注册
    @PostMapping("/register")
    public Result<String> register(@Validated @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success("用户注册成功");
    }

    //用户登录
    @PostMapping("/login")
    public Result<UserLoginVO> login(@Validated @RequestBody LoginDTO loginDTO) {
        UserLoginVO userLoginVO = userService.login(loginDTO);
        return Result.success(userLoginVO);
    }

    //用户登出
    @PostMapping("/logout")
    public Result<String> logout(){
        Long userId = UserContext.getCurUserId();
        userOnlineStatusService.setUserOffline(userId);
        return Result.success("用户登出成功");
    }

    //获取当前登录用户信息
    @GetMapping("/me")
    public Result<UserLoginVO> me(){
        Long userId = UserContext.getCurUserId();
        UserLoginVO userLoginVO = userService.getUserLoginVO(userId);
        return Result.success(userLoginVO);
    }

}
