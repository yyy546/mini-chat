package com.minichat.space.task;

import com.minichat.space.mapper.SpaceCommentMapper;
import com.minichat.space.mapper.SpaceLikeMapper;
import com.minichat.space.mapper.SpacePostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class SpaceCleanupTask {

    private final SpacePostMapper spacePostMapper;
    private final SpaceCommentMapper spaceCommentMapper;
    private final SpaceLikeMapper spaceLikeMapper;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(cron = "0 0 22 * * ?")
    public void cleanupExpireData(){
        log.info("开始执行回收站数据清理任务");
        LocalDateTime expireTime = LocalDateTime.now().minusDays(14);
        int limit = 1000;

        while (true) {
            List<Long> expirePostIds = spacePostMapper.selectExpirePostIds(expireTime, limit);

            if(expirePostIds != null && !expirePostIds.isEmpty()){
                log.info("过期帖子id:{}, 开始删除帖子及其关联评论及点赞记录", expirePostIds);
                
                transactionTemplate.execute(status -> {
                    try {
                        //删除关联评论及点赞记录
                        spaceCommentMapper.deleteBatchByPostIds(expirePostIds);
                        spaceLikeMapper.deleteBatchByPostIds(expirePostIds);
                        //删除帖子
                        spacePostMapper.deleteBatchByIds(expirePostIds);
                        return true;
                    } catch (Exception e) {
                        log.error("清理过期数据异常", e);
                        status.setRollbackOnly();
                        throw e;
                    }
                });
                
                log.info("过期帖子id:{} 已删除", expirePostIds);
                
                if (expirePostIds.size() < limit) {
                    break;
                }
            } else {
                log.info("暂无过期帖子");
                break;
            }
        }
    }
}
