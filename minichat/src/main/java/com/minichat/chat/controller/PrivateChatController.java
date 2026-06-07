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

@Slf4j
@RestController
@RequiredArgsConstructor
public class PrivateChatController {

    private final PrivateMessageService privateMessageService;

    @Autowired
    @Lazy
    private UserOnlineStatusService userOnlineStatusService;

    @SensitiveFilter(replacement = "***")
    @MessageMapping("/chat.private")
    public Result<String> sendPrivateMessage(@Valid @Payload PrivateMessageDTO privateMessageDTO, Principal principal) {
        if (principal == null) {
            return Result.error("用户未登录");
        }
        String msg = privateMessageService.sendPrivateMessage(privateMessageDTO, principal);
        return Result.success(msg);
    }

    @PostMapping("/chat/private/upload")
    public Result<FileVO> uploadPrivateFile(@RequestPart("file") MultipartFile file, @RequestParam("type") Integer type) {
        FileVO fileVO = privateMessageService.uploadPrivateFile(file, type);
        return Result.success(fileVO);
    }

    @MessageMapping("/heartbeat")
    public void handleHeartbeat(Principal principal){
        if(principal != null && principal.getName() != null){
            try{
                Long userId = Long.valueOf(principal.getName());
                userOnlineStatusService.renewUserOnlineStatus(userId);

                log.info("用户 {} 刷新了在线状态", userId);
            }catch(NumberFormatException e){
                log.info("用户ID {} 格式错误", principal.getName());
            }
        }else{
                log.info("用户ID不能为空");
        }
    }

    @GetMapping("/chat/private/history")
    public Result<IPage<PrivateMessageVO>> getPrivateMessageHistory(@RequestParam("userId") Long userId,
                                                                     @RequestParam(value = "page", defaultValue = "1") Integer page,
                                                                     @RequestParam(value = "pageSize", defaultValue = "50") Integer pageSize) {
        Long currentUserId = UserContext.getCurUserId();
        if (userId == null || userId <= 0) {
            return Result.error("聊天对象ID不能为空");
        }
        IPage<PrivateMessageVO> history = privateMessageService.getPrivateMessageHistory(currentUserId, userId, page, pageSize);
        return Result.success(history);
    }

    @PostMapping("/chat/private/mark-read")
    public Result<String> markMessagesAsRead(@RequestParam("receiverId") Long receiverId) {
        Long currentUserId = UserContext.getCurUserId();
        if (receiverId == null || receiverId <= 0) {
            return Result.error("聊天对象ID不能为空");
        }
        privateMessageService.markMessagesAsRead(currentUserId, receiverId);
        return Result.success("消息标记为已读");
    }

    @PostMapping("/chat/private/recall")
    public Result<String> recallPrivateMessage(@RequestParam("messageId") Long messageId){
        Long currentUserId = UserContext.getCurUserId();
        if (messageId == null || messageId <= 0) {
            return Result.error("消息ID不能为空");
        }
        privateMessageService.recallPrivateMessage(currentUserId, messageId);
        return Result.success("消息撤回成功");
    }

}
