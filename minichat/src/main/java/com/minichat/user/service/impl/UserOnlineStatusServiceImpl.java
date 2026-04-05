package com.minichat.user.service.impl;

import com.minichat.user.dto.UserStatusChangeDTO;
import com.minichat.common.constants.RedisConstants;
import com.minichat.user.service.UserOnlineStatusService;
import com.minichat.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserOnlineStatusServiceImpl implements UserOnlineStatusService {

    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void setUserOnline(Long userId) {
        try {
            String key = RedisConstants.USER_ONLINE_USER_KEY_PREFIX + userId;
            // 使用字符串 "true" 存储，便于在 Redis 中查看明文
            redisTemplate.opsForValue().set(key, "true", RedisConstants.ONLINE_STATUS_EXPIRE_TIME, TimeUnit.SECONDS);
            // 推送用户状态变更（立即推送，确保其他用户能及时看到状态变化）
            pushStatusChange(userId, true);
            log.info("用户 {} 已设置为在线，状态变更已推送", userId);
        } catch (IllegalStateException e) {
            // 应用关闭时 Redis 连接可能已停止，忽略此错误
            log.warn("应用关闭时无法更新用户在线状态: userId={}, error={}", userId, e.getMessage());
        } catch (Exception e) {
            // 其他异常也记录但不抛出，避免影响应用关闭流程
            log.error("设置用户在线状态失败: userId={}", userId, e);
        }
    }


    @Override
    public Boolean isUserOnline(Long userId) {
        String key = RedisConstants.USER_ONLINE_USER_KEY_PREFIX + userId;
        return redisTemplate.hasKey(key);
    }

    @Override
    public Set<String> getAllOnlineUserIds() {
        Set<String> keys = redisTemplate.keys(RedisConstants.USER_ONLINE_USER_KEY_PREFIX + "*");
        return keys.stream()
                .map(key -> key.substring(RedisConstants.USER_ONLINE_USER_KEY_PREFIX.length()))
                .collect(Collectors.toSet());
    }

    @Override
    public void setUserOffline(Long userId) {
        try {
            String key = RedisConstants.USER_ONLINE_USER_KEY_PREFIX + userId;
            redisTemplate.delete(key);
            // 推送用户状态变更（立即推送，确保其他用户能及时看到状态变化）
            pushStatusChange(userId, false);
            log.info("用户 {} 已设置为离线，状态变更已推送", userId);
        } catch (IllegalStateException e) {
            // 应用关闭时 Redis 连接可能已停止，忽略此错误
            log.warn("应用关闭时无法更新用户离线状态: userId={}, error={}", userId, e.getMessage());
        } catch (Exception e) {
            // 其他异常也记录但不抛出，避免影响应用关闭流程
            log.error("设置用户离线状态失败: userId={}", userId, e);
        }
    }

    @Override
    public void renewUserOnlineStatus(Long userId) {
        try {
            String key = RedisConstants.USER_ONLINE_USER_KEY_PREFIX + userId;
            if(Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                redisTemplate.expire(key, RedisConstants.ONLINE_STATUS_EXPIRE_TIME, TimeUnit.SECONDS);
            }else{
                // 键已过期，相当于重新上线，推送在线状态
                setUserOnline(userId);
            }
        } catch (IllegalStateException e) {
            // 应用关闭时 Redis 连接可能已停止，忽略此错误
            log.warn("应用关闭时无法续期用户在线状态: userId={}, error={}", userId, e.getMessage());
        } catch (Exception e) {
            // 其他异常也记录但不抛出，避免影响应用关闭流程
            log.error("续期用户在线状态失败: userId={}", userId, e);
        }
    }

    private void pushStatusChange(Long userId, Boolean isOnline) {
        try {
            UserStatusChangeDTO userStatusChangeDTO = UserStatusChangeDTO.builder()
                    .userId(userId)
                    .nickname(userService.getNicknameById(userId))
                    .isOnline(isOnline)
                    .build();

            messagingTemplate.convertAndSend(RedisConstants.STATUS_TOPIC, userStatusChangeDTO);
            log.info("已推送用户状态变更: userId={}, isOnline={}", userId, isOnline);
        } catch (Exception e) {
            log.error("推送用户状态变更失败: userId={}", userId, e);
        }
    }

}
