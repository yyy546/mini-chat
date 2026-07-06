package com.minichat.space.listener;


import com.minichat.common.cache.CacheKeys;
import com.minichat.space.constants.FeedConstants;
import com.minichat.common.mq.MqConstants;
import com.minichat.friend.service.FriendService;
import com.minichat.friend.vo.FriendVO;
import com.minichat.space.dto.SpacePostMqDTO;
import com.minichat.space.mapper.SpacePostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class SpacePostListener {

    private final RedisTemplate<String, Object> redisTemplate;
    private final FriendService friendService;
    private final SpacePostMapper spacePostMapper;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstants.SPACE_POST_QUEUE, durable = "true",
            arguments = {
                    @Argument(name = "x-dead-letter-exchange", value = MqConstants.SPACE_POST_DLX_EXCHANGE),
                    @Argument(name = "x-dead-letter-routing-key", value = MqConstants.SPACE_POST_DLX_ROUTING_KEY),
            }),
            exchange = @Exchange(value = MqConstants.SPACE_POST_EXCHANGE, type = ExchangeTypes.TOPIC, durable = "true"),
            key = {MqConstants.SPACE_POST_ROUTING_KEY, MqConstants.SPACE_POST_RECOVER_ROUTING_KEY}
    ))
    public void publish(SpacePostMqDTO spacePostMqDTO) {
        log.info("空间帖子推送给好友, spacePostIdMap: {}", spacePostMqDTO);
        try{
            Long authorId = spacePostMqDTO.getAuthorId();
            Long spacePostId = spacePostMqDTO.getSpacePostId();
            Long timestamp = spacePostMqDTO.getTimestamp();

            // 获取好友列表并添加作者ID
            List<Long> followedUserIds = new ArrayList<>(friendService.getFriendList(authorId).stream()
                    .map(FriendVO::getFriendId)
                    .toList());
            followedUserIds.add(authorId);

            followedUserIds.forEach(followedUserId -> {
                redisTemplate.opsForZSet().add(CacheKeys.feedFollowed(followedUserId), spacePostId, timestamp );
                //修剪zset, 保留最新的500个
                redisTemplate.opsForZSet().removeRange(CacheKeys.feedFollowed(followedUserId), 0, FeedConstants.FEED_TRIM_END_INDEX);
            });
                log.info("空间帖子 {} 推送给好友成功", spacePostId);
        } catch (Exception e) {
            log.error("空间帖子推送给好友失败", e);
        }

    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstants.SPACE_POST_DELETE_QUEUE, durable = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = MqConstants.SPACE_POST_DLX_EXCHANGE),
                            @Argument(name = "x-dead-letter-routing-key", value = MqConstants.SPACE_POST_DLX_ROUTING_KEY),
                    }),
            exchange = @Exchange(value = MqConstants.SPACE_POST_EXCHANGE, type = ExchangeTypes.TOPIC, durable = "true"),
            key = MqConstants.SPACE_POST_DELETE_ROUTING_KEY
    ))
    public void delete(SpacePostMqDTO spacePostMqDTO){
        log.info("空间帖子删除,从好友的zset中删除, spacePostIdMap: {}", spacePostMqDTO);
        try{
            Long authorId = spacePostMqDTO.getAuthorId();
            Long spacePostId = spacePostMqDTO.getSpacePostId();

            // 获取好友列表并添加作者ID
            List<Long> followedUserIds = new ArrayList<>(friendService.getFriendList(authorId).stream()
                    .map(FriendVO::getFriendId)
                    .toList());
            followedUserIds.add(authorId);

            followedUserIds.forEach(followedUserId -> {
                redisTemplate.opsForZSet().remove(CacheKeys.feedFollowed(followedUserId), spacePostId);
            });
                log.info("空间帖子 {} 从好友的zset中删除成功", spacePostId);
        } catch (Exception e) {
            log.error("空间帖子从好友的zset中删除失败", e);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstants.SPACE_POST_DELETEALL_QUEUE, durable = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = MqConstants.SPACE_POST_DLX_EXCHANGE),
                            @Argument(name = "x-dead-letter-routing-key", value = MqConstants.SPACE_POST_DLX_ROUTING_KEY),
                    }),
            exchange = @Exchange(value = MqConstants.SPACE_POST_EXCHANGE, type = ExchangeTypes.TOPIC, durable = "true"),
            key = MqConstants.SPACE_POST_DELETEALL_ROUTING_KEY
    ))
    public void deleteAll(SpacePostMqDTO spacePostMqDTO){
        try{
            Long authorId = spacePostMqDTO.getAuthorId();
            Long targetUserId = spacePostMqDTO.getTargetUserId();
            log.info("删除好友所有空间帖子, authorId: {}, targetUserId: {}", authorId, targetUserId);
            
            // 查询该作者的所有帖子ID
            List<Long> postIds = spacePostMapper.selectPostIdsByAuthorId(authorId);
            if(postIds == null || postIds.isEmpty()){
                return;
            }
            
            // 从目标用户的Feed流中移除这些帖子
            redisTemplate.opsForZSet().remove(CacheKeys.feedFollowed(targetUserId), postIds.toArray());
            
            log.info("删除好友所有空间帖子成功, authorId: {}, targetUserId: {}", authorId, targetUserId);
        }catch(Exception e){
            log.error("删除好友所有空间帖子失败", e);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstants.SPACE_POST_DLX_QUEUE, durable = "true"),
            exchange = @Exchange(value = MqConstants.SPACE_POST_DLX_EXCHANGE, type = ExchangeTypes.TOPIC, durable = "true"),
            key = MqConstants.SPACE_POST_DLX_ROUTING_KEY
    ))
    public void handlePublishDLX(SpacePostMqDTO spacePostMqDTO) {
        log.info("空间帖子推送给好友失败, spacePostIdMap: {}", spacePostMqDTO);
    }

}
