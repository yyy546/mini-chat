package com.minichat.user.service.impl;

import com.minichat.common.cache.CacheKeys;
import com.minichat.user.dto.UserStatusChangeDTO;
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
            String key = CacheKeys.userOnline(userId);
            redisTemplate.opsForValue().set(key, "true", CacheKeys.EXPIRE_ONLINE_SECONDS, TimeUnit.SECONDS);
            pushStatusChange(userId, true);
            log.info("用户 {} 已设置为在线，状态变更已推送", userId);
        } catch (IllegalStateException e) {
            log.warn("应用关闭时无法更新用户在线状态: userId={}, error={}", userId, e.getMessage());
        } catch (Exception e) {
            log.error("设置用户在线状态失败: userId={}", userId, e);
        }
    }

    @Override
    public Boolean isUserOnline(Long userId) {
        String key = CacheKeys.userOnline(userId);
        return redisTemplate.hasKey(key);
    }

    @Override
    public Set<String> getAllOnlineUserIds() {
        Set<String> keys = redisTemplate.keys(CacheKeys.USER_ONLINE_PREFIX + "*");
        return keys.stream()
                .map(key -> key.substring(CacheKeys.USER_ONLINE_PREFIX.length()))
                .collect(Collectors.toSet());
    }

    @Override
    public void setUserOffline(Long userId) {
        try {
            String key = CacheKeys.userOnline(userId);
            redisTemplate.delete(key);
            pushStatusChange(userId, false);
            log.info("用户 {} 已设置为离线，状态变更已推送", userId);
        } catch (IllegalStateException e) {
            log.warn("应用关闭时无法更新用户离线状态: userId={}, error={}", userId, e.getMessage());
        } catch (Exception e) {
            log.error("设置用户离线状态失败: userId={}", userId, e);
        }
    }

    @Override
    public void renewUserOnlineStatus(Long userId) {
        try {
            String key = CacheKeys.userOnline(userId);
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                redisTemplate.expire(key, CacheKeys.EXPIRE_ONLINE_SECONDS, TimeUnit.SECONDS);
            } else {
                setUserOnline(userId);
            }
        } catch (IllegalStateException e) {
            log.warn("应用关闭时无法续期用户在线状态: userId={}, error={}", userId, e.getMessage());
        } catch (Exception e) {
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

            messagingTemplate.convertAndSend(CacheKeys.STATUS_TOPIC, userStatusChangeDTO);
            log.info("已推送用户状态变更: userId={}, isOnline={}", userId, isOnline);
        } catch (Exception e) {
            log.error("推送用户状态变更失败: userId={}", userId, e);
        }
    }

}
