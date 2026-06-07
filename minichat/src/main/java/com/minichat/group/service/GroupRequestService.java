package com.minichat.group.service;

import com.minichat.group.dto.GroupRequestDTO;
import com.minichat.group.dto.HandleGroupRequestDTO;
import com.minichat.group.vo.ReceivedGroupRequestVO;
import com.minichat.group.vo.SentGroupRequestVO;
import jakarta.validation.Valid;

import java.util.List;

public interface GroupRequestService {
    String sendGroupRequest(@Valid GroupRequestDTO groupRequestDTO);

    List<SentGroupRequestVO> getSentGroupRequestList(Long currentUserId);

    List<ReceivedGroupRequestVO> getReceivedGroupRequestList(Long currentUserId);

    void handleGroupRequest(@Valid HandleGroupRequestDTO handleGroupRequestDTO);
}
