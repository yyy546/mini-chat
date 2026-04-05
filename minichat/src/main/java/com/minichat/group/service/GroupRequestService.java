package com.minichat.group.service;

import com.minichat.group.dto.GroupRequestDTO;
import com.minichat.group.dto.HandleGroupRequestDTO;
import com.minichat.group.vo.ReceivedGroupRequestVO;
import com.minichat.group.vo.SentGroupRequestVO;
import com.minichat.common.result.Result;
import jakarta.validation.Valid;

import java.util.List;

public interface GroupRequestService {
    /**
     * 发送群聊加入请求
     * @param groupRequestDTO 群聊加入请求DTO
     * @return 操作结果
     */
    Result<String> sendGroupRequest(@Valid GroupRequestDTO groupRequestDTO);

    /**
     * 获取已发送的群聊加入请求列表
     * @param currentUserId 当前用户ID
     * @return 已发送的群聊加入请求列表
     */
    List<SentGroupRequestVO> getSentGroupRequestList(Long currentUserId);

    /**
     * 获取已接收的群聊加入请求列表
     * @param currentUserId 当前用户ID
     * @return 已接收的群聊加入请求列表
     */
    List<ReceivedGroupRequestVO> getReceivedGroupRequestList(Long currentUserId);

     /**
     * 处理群聊加入请求
     * @param handleGroupRequestDTO 处理群聊加入请求DTO
     * @return 操作结果
     */
    Result<String> handleGroupRequest(@Valid HandleGroupRequestDTO handleGroupRequestDTO);
}
