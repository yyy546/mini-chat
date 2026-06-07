package com.minichat.chat.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.minichat.chat.dto.GroupMessageDTO;
import com.minichat.chat.vo.FileVO;
import com.minichat.chat.vo.GroupMessageVO;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

public interface GroupMessageService {
    String sendGroupMessage(@Valid GroupMessageDTO groupMessageDTO, Principal principal);

    IPage<GroupMessageVO> getGroupMessageHistory(Long groupId, Integer page, Integer pageSize);

    FileVO uploadGroupFile(MultipartFile file, Integer type);

    void markGroupMessageRead(Long groupId);

    void recallGroupMessage(Long groupId, Long messageId);
}
