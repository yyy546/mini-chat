package com.minichat.chat.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.minichat.chat.dto.PrivateMessageDTO;
import com.minichat.chat.vo.FileVO;
import com.minichat.chat.vo.PrivateMessageVO;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

public interface PrivateMessageService {
    String sendPrivateMessage(PrivateMessageDTO privateMessageDTO, Principal principal);

    IPage<PrivateMessageVO> getPrivateMessageHistory(Long currentUserId, Long targetUserId, Integer page, Integer pageSize);

    void markMessagesAsRead(Long currentUserId, Long receiverId);

    FileVO uploadPrivateFile(MultipartFile file, Integer type);

    void recallPrivateMessage(Long currentUserId, Long messageId);
}
