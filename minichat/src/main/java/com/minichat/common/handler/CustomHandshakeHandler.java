package com.minichat.common.handler;

import com.minichat.user.entity.User;
import com.minichat.user.mapper.UserMapper;
import com.minichat.user.service.UserOnlineStatusService;
import com.minichat.common.util.JwtUtil;
import com.minichat.common.util.UserPrincipal;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.WebSocketHandler;

import java.security.Principal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Autowired
    @Lazy
    private UserOnlineStatusService userOnlineStatusService;

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = null;

        // 1. 安全获取ServletRequest（避免强制转换异常）
        ServletServerHttpRequest servletRequest;
        if (request instanceof ServletServerHttpRequest) {
            servletRequest = (ServletServerHttpRequest) request;
        } else {
            throw new HandshakeFailureException("WebSocket握手失败：不支持的请求类型");
        }

        // 2. 优先从请求头获取Token（处理Bearer前缀），其次从URL参数获取
        // 2.1 从Authorization头获取
        String authHeader = servletRequest.getServletRequest().getHeader("Authorization");
        if (authHeader != null && !authHeader.isEmpty()) {
            // 处理Bearer前缀：如果以Bearer开头，截取后面的Token部分
            if (authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7).trim(); // 去掉"Bearer "前缀并去空格
            } else {
                token = authHeader.trim(); // 无前缀则直接使用
            }
        }

        // 2.2 若请求头无Token，从URL参数获取
        if (token == null || token.isEmpty()) {
            token = servletRequest.getServletRequest().getParameter("token");
        }

        // 3. 校验Token是否存在
        if (token == null || token.isEmpty()) {
            throw new HandshakeFailureException("WebSocket握手失败：未提供Token（Header或参数）");
        }

        try {
            // 4. 解析JWT，区分具体异常类型
            Long userId;
            try {
                userId = jwtUtil.getUserIdFromToken(token);
            } catch (ExpiredJwtException e) {
                throw new HandshakeFailureException("WebSocket握手失败：Token已过期", e);
            } catch (Exception e) {
                throw new HandshakeFailureException("WebSocket握手失败：Token格式错误", e);
            }

            // 5. 查询用户并校验状态
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new HandshakeFailureException("WebSocket握手失败：用户不存在（用户ID：" + userId + "）");
            }
            // 修复注释歧义：明确0=正常，1=禁用，判断状态为1时抛异常（逻辑与原代码一致，仅优化注释）
            if (user.getStatus() == 1) {
                throw new HandshakeFailureException("WebSocket握手失败：用户已被禁用（用户ID：" + userId + "）");
            }

            // 6. 校验用户ID有效性
            if (userId == null) {
                throw new HandshakeFailureException("WebSocket握手失败：用户ID不存在（用户ID：" + userId + "）");
            }

            // 7. 设置用户在线状态
            userOnlineStatusService.setUserOnline(userId);

            return new UserPrincipal(userId);

        } catch (HandshakeFailureException e) {
            // 直接抛出已定义的具体异常
            throw e;
        } catch (Exception e) {
            throw new HandshakeFailureException("WebSocket握手失败：服务器内部错误", e);
        }
    }
}