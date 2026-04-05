package com.minichat.chat.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.minichat.chat.dto.GroupMessageDTO;
import com.minichat.chat.vo.FileVO;
import com.minichat.chat.vo.GroupMessageVO;
import com.minichat.common.annotation.SensitiveFilter;
import com.minichat.common.result.Result;
import com.minichat.chat.service.GroupMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@Slf4j
@RestController
@RequiredArgsConstructor
public class GroupChatController {

    private final GroupMessageService groupMessageService;

    //发送群聊消息
    @SensitiveFilter(replacement = "***")
    @MessageMapping("/chat.group")
    public Result<String> sendGroupMessage(@Valid @Payload GroupMessageDTO groupMessageDTO, Principal principal) {
        if (principal == null) {
            return Result.error("用户未登录");
        }
        return groupMessageService.sendGroupMessage(groupMessageDTO, principal);
    }

    //查看群聊消息历史记录
    @GetMapping("/chat/group/history")
    public Result<IPage<GroupMessageVO>> getGroupMessageHistory(@RequestParam("groupId") Long groupId,
                                                                @RequestParam(value = "page", defaultValue = "1") Integer page,
                                                                @RequestParam(value = "pageSize", defaultValue = "50") Integer pageSize) {
        // 校验参数
        if (groupId == null || groupId <= 0) {
            return Result.error("群聊ID不能为空");
        }
        return groupMessageService.getGroupMessageHistory(groupId, page, pageSize);
    }

    // 上传图片或文件到群聊
    @PostMapping("/chat/group/upload")
    public Result<FileVO> uploadGroupFile(@RequestPart("file") MultipartFile file, @RequestParam("type") Integer type) {
        return groupMessageService.uploadGroupFile(file, type);
    }

    // 标记群聊消息为已读
    @PostMapping("/chat/group/mark-read")
    public Result<String> markGroupMessageRead(@RequestParam("groupId") Long groupId) {
        if (groupId == null || groupId <= 0) {
            return Result.error("群聊ID不能为空");
        }
        return groupMessageService.markGroupMessageRead(groupId);
    }

    // 撤回群聊消息
    @PostMapping("/chat/group/recall")
    public Result<String> recallGroupMessage(@RequestParam("groupId") Long groupId, @RequestParam("messageId") Long messageId) {
        if (groupId == null || groupId <= 0) {
            return Result.error("群聊ID不能为空");
        }
        if (messageId == null || messageId <= 0) {
            return Result.error("消息ID不能为空");
        }
        return groupMessageService.recallGroupMessage(groupId, messageId);
    }
}
