package com.minichat.chat.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.minichat.chat.vo.FileVO;
import com.minichat.chat.vo.PrivateMessageVO;
import com.minichat.chat.dto.PrivateMessageDTO;
import com.minichat.common.annotation.SensitiveFilter;
import com.minichat.common.result.Result;
import com.minichat.chat.service.PrivateMessageService;
import com.minichat.user.service.UserOnlineStatusService;
import com.minichat.common.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PrivateChatController {

    private final PrivateMessageService privateMessageService;

    @Autowired
    @Lazy
    private UserOnlineStatusService userOnlineStatusService;

    // 发送文本私聊消息
    @SensitiveFilter(replacement = "***")
    @MessageMapping("/chat.private")
    public Result<String> sendPrivateMessage(@Valid @Payload PrivateMessageDTO privateMessageDTO, Principal principal) {
        if (principal == null) {
            return Result.error("用户未登录");
        }
        return privateMessageService.sendPrivateMessage(privateMessageDTO, principal);
    }

    // 上传图片或文件
    @PostMapping("/chat/private/upload")
    public Result<FileVO> uploadPrivateFile(@RequestPart("file") MultipartFile file, @RequestParam("type") Integer type) {
        return privateMessageService.uploadPrivateFile(file, type);
    }

    // 处理心跳消息
    @MessageMapping("/heartbeat")
    public void handleHeartbeat(Principal principal){
        if(principal != null && principal.getName() != null){
            try{
                Long userId = Long.valueOf(principal.getName());
                // 刷新用户在线状态
                userOnlineStatusService.renewUserOnlineStatus(userId);

                log.info("用户 {} 刷新了在线状态", userId);
            }catch(NumberFormatException e){
                log.info("用户ID {} 格式错误", principal.getName());
            }
        }else{
                log.info("用户ID不能为空");
        }
    }

    // 获取私聊消息历史记录
    @GetMapping("/chat/private/history")
    public Result<IPage<PrivateMessageVO>> getPrivateMessageHistory(@RequestParam("userId") Long userId,
                                                                     @RequestParam(value = "page", defaultValue = "1") Integer page,
                                                                     @RequestParam(value = "pageSize", defaultValue = "50") Integer pageSize) {
        Long currentUserId = UserContext.getCurUserId();
        // 校验参数
        if (userId == null || userId <= 0) {
            return Result.error("聊天对象ID不能为空");
        }
        return privateMessageService.getPrivateMessageHistory(currentUserId, userId, page, pageSize);
    }

    // 标记私聊消息为已读
    @PostMapping("/chat/private/mark-read")
    public Result<String> markMessagesAsRead(@RequestParam("receiverId") Long receiverId) {
        Long currentUserId = UserContext.getCurUserId();
        // 校验参数
        if (receiverId == null || receiverId <= 0) {
            return Result.error("聊天对象ID不能为空");
        }
        return privateMessageService.markMessagesAsRead(currentUserId, receiverId);
    }

    // 撤回私聊消息
    @PostMapping("/chat/private/recall")
    public Result<String> recallPrivateMessage(@RequestParam("messageId") Long messageId){
        Long currentUserId = UserContext.getCurUserId();
        // 校验参数
        if (messageId == null || messageId <= 0) {
            return Result.error("消息ID不能为空");
        }
        return privateMessageService.recallPrivateMessage(currentUserId, messageId);
    }

}
