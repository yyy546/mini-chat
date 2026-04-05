package com.minichat.chat.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.minichat.chat.dto.PrivateMessageDTO;
import com.minichat.chat.vo.FileVO;
import com.minichat.chat.vo.PrivateMessageVO;
import com.minichat.common.result.Result;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

public interface PrivateMessageService {
    /**
     * 发送私聊消息
     */
    Result<String> sendPrivateMessage(PrivateMessageDTO privateMessageDTO, Principal principal);

    /**
     * 获取私聊消息历史记录
     */
    Result<IPage<PrivateMessageVO>> getPrivateMessageHistory(Long currentUserId, Long targetUserId, Integer page, Integer pageSize);

    /**
     * 标记私聊消息为已读
     */
    Result<String> markMessagesAsRead(Long currentUserId, Long receiverId);


    /**
     * 上传私聊文件（图片或其他文件）
     */
    Result<FileVO> uploadPrivateFile(MultipartFile file, Integer type);

    /**
     * 撤回私聊消息
     */
    Result<String> recallPrivateMessage(Long currentUserId, Long messageId);
}
