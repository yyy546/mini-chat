package com.minichat.chat.controller;

import com.minichat.chat.entity.EsChatMessage;
import com.minichat.chat.service.ChatSearchService;
import com.minichat.common.result.Result;
import com.minichat.common.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/chat/search")
@RequiredArgsConstructor
public class ChatSearchController {

    private final ChatSearchService chatSearchService;

    @GetMapping
    public Result<List<EsChatMessage>> searchChatMessages(@RequestParam String keyword,
                                                          @RequestParam Integer type, @RequestParam Long targetId){
        Long currentUserId = UserContext.getCurUserId();
        List<EsChatMessage> esChatMessages = chatSearchService.search(keyword, type, targetId, currentUserId);
        return Result.success(esChatMessages);
    }
}
