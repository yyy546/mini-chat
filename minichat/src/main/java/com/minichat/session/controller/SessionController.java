package com.minichat.session.controller;

import com.minichat.session.vo.SessionVO;
import com.minichat.common.result.Result;
import com.minichat.session.service.SessionService;
import com.minichat.common.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    // 获取会话列表
    @GetMapping("/list")
    public Result<List<SessionVO>> getSessionList(){
        Long currentUserId = UserContext.getCurUserId();
        if(currentUserId == null){
            return Result.error("用户未登录");
        }
        List<SessionVO> sessionVOList = sessionService.getSessionList(currentUserId);
        return Result.success("获取会话列表成功", sessionVOList);
    }

}
