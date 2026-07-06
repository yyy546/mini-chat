package com.minichat.session.controller;

import com.minichat.common.core.exception.AuthException;
import com.minichat.common.core.result.Result;
import com.minichat.common.security.jwt.UserContext;
import com.minichat.session.service.SessionService;
import com.minichat.session.vo.SessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping("/list")
    public Result<List<SessionVO>> getSessionList(){
        Long currentUserId = UserContext.getCurUserId();
        if(currentUserId == null){
            throw new AuthException("用户未登录");
        }
        List<SessionVO> sessionVOList = sessionService.getSessionList(currentUserId);
        return Result.success("获取会话列表成功", sessionVOList);
    }

}
