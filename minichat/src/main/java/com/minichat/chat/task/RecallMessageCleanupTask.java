package com.minichat.chat.task;

import com.minichat.chat.mapper.GroupMessageMapper;
import com.minichat.chat.mapper.PrivateMessageMapper;
import com.minichat.common.core.constants.MessageConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RecallMessageCleanupTask {

    private final GroupMessageMapper groupMessageMapper;
    private final PrivateMessageMapper privateMessageMapper;

    /**
     * 定时任务：清理过期的撤回消息
     */
    @Scheduled(cron = "0 0/10 * * * ?") // 每10分钟执行一次
    public void cleanupExpiredRecallMessages() {
        log.info("开始清理过期的撤回消息");
        LocalDateTime now = LocalDateTime.now();
        // 清理过期的撤回消息
        List<Long> recallPrivateMessageIds = privateMessageMapper.selectExpiredRecallMessageIds(now.minusMinutes(MessageConstants.MESSAGE_RECALL_TIME_LIMIT));
        if (recallPrivateMessageIds != null && !recallPrivateMessageIds.isEmpty()) {
            privateMessageMapper.deleteBatch(recallPrivateMessageIds);
        }
        // 清理过期的撤回消息
        List<Long> recallGroupMessageIds = groupMessageMapper.selectExpiredRecallMessageIds(now.minusMinutes(MessageConstants.MESSAGE_RECALL_TIME_LIMIT));
        if (recallGroupMessageIds != null && !recallGroupMessageIds.isEmpty()) {
            groupMessageMapper.deleteBatch(recallGroupMessageIds);
        }
        log.info("清理过期的撤回消息完成，清理了 {} 条私聊消息和 {} 条群聊消息",
                recallPrivateMessageIds == null ? 0 : recallPrivateMessageIds.size(),
                recallGroupMessageIds == null ? 0 : recallGroupMessageIds.size());
    }
}
