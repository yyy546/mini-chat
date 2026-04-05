package com.minichat.common.listener;

import com.minichat.user.service.UserOnlineStatusService;
import com.minichat.common.util.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebsocketEventListener {

    private final UserOnlineStatusService userOnlineStatusService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        try {
            Principal principal = event.getUser();
            if (principal instanceof UserPrincipal) {
                Long userId = ((UserPrincipal) principal).getUserId();
                userOnlineStatusService.setUserOnline(userId);
            }
        } catch (Exception e) {
            // 捕获所有异常，避免影响 WebSocket 连接流程
            log.error("处理 WebSocket 连接事件失败", e);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        try {
            Principal principal = event.getUser();
            if (principal instanceof UserPrincipal) {
                Long userId = ((UserPrincipal) principal).getUserId();
                userOnlineStatusService.setUserOffline(userId);
            }
        } catch (Exception e) {
            // 捕获所有异常，特别是应用关闭时的 Redis 连接错误
            // 避免影响应用关闭流程
            log.warn("处理 WebSocket 断开事件失败（可能是应用关闭）: {}", e.getMessage());
        }
    }
}
