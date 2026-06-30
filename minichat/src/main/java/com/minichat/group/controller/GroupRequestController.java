package com.minichat.group.controller;

import com.minichat.common.exception.AuthException;
import com.minichat.common.result.Result;
import com.minichat.common.util.UserContext;
import com.minichat.group.dto.GroupRequestDTO;
import com.minichat.group.dto.HandleGroupRequestDTO;
import com.minichat.group.service.GroupRequestService;
import com.minichat.group.vo.ReceivedGroupRequestVO;
import com.minichat.group.vo.SentGroupRequestVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/group/request")
@RequiredArgsConstructor
public class GroupRequestController {

    private final GroupRequestService groupRequestService;

    @PostMapping("/send")
    public Result<String> sendGroupRequest(@Valid @RequestBody GroupRequestDTO groupRequestDTO) {
        Long currentUserId = UserContext.getCurUserId();
        if (currentUserId == null) {
            throw new AuthException("用户未登录");
        }
        String msg = groupRequestService.sendGroupRequest(groupRequestDTO);
        return Result.success(msg);
    }

    @GetMapping("/sent")
    public Result<List<SentGroupRequestVO>> getSentGroupRequests() {
        Long currentUserId = UserContext.getCurUserId();
        if (currentUserId == null) {
            throw new AuthException("用户未登录");
        }
        List<SentGroupRequestVO> sentGroupRequestVOList = groupRequestService.getSentGroupRequestList(currentUserId);
        return Result.success(sentGroupRequestVOList);
    }

    @PostMapping("/handle")
    public Result<String> handleGroupRequest(@Valid @RequestBody HandleGroupRequestDTO handleGroupRequestDTO) {
        Long currentUserId = UserContext.getCurUserId();
        if (currentUserId == null) {
            throw new AuthException("用户未登录");
        }
        groupRequestService.handleGroupRequest(handleGroupRequestDTO);
        return Result.success("处理成功");
    }

    @GetMapping("/list")
    public Result<List<ReceivedGroupRequestVO>> getReceivedGroupRequests() {
        Long currentUserId = UserContext.getCurUserId();
        if (currentUserId == null) {
            throw new AuthException("用户未登录");
        }
        List<ReceivedGroupRequestVO> receivedGroupRequestVOList = groupRequestService.getReceivedGroupRequestList(currentUserId);
        return Result.success(receivedGroupRequestVOList);
    }

}
