package com.minichat.chat.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.minichat.chat.dto.GroupMessageDTO;
import com.minichat.chat.vo.FileVO;
import com.minichat.chat.vo.GroupMessageVO;
import com.minichat.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

public interface GroupMessageService {
    /**
     * 发送群消息
     */
    Result<String> sendGroupMessage(@Valid GroupMessageDTO groupMessageDTO, Principal principal);

    /**
     * 获取群消息历史记录
     */
    Result<IPage<GroupMessageVO>> getGroupMessageHistory(Long groupId, Integer page, Integer pageSize);

    /**
     * 上传群文件
     */
    Result<FileVO> uploadGroupFile(MultipartFile file, Integer type);

    /**
     * 标记群消息为已读
     */
    Result<String> markGroupMessageRead(Long groupId);

     /**
     * 撤回群聊消息
     */
    Result<String> recallGroupMessage(Long groupId, Long messageId);
}
