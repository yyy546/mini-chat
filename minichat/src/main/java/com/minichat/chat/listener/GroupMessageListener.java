package com.minichat.chat.listener;

import com.minichat.chat.dto.GroupMessageMQDTO;
import com.minichat.chat.dto.RecallMessageDTO;
import com.minichat.chat.entity.EsChatMessage;
import com.minichat.chat.service.ChatSearchService;
import com.minichat.chat.vo.GroupMessageVO;
import com.minichat.common.core.constants.MessageConstants;
import com.minichat.common.mq.MqConstants;
import com.minichat.common.core.constants.SessionConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class GroupMessageListener {


    private final SimpMessagingTemplate messagingTemplate;
    private final ChatSearchService chatSearchService;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 处理群聊消息业务
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConstants.GROUP_QUEUE, durable = "true",
            arguments = {
                    @Argument(name = "x-dead-letter-exchange", value = MqConstants.GROUP_DLX_EXCHANGE),
                    @Argument(name = "x-dead-letter-routing-key", value = MqConstants.GROUP_DLX_ROUTING_KEY)
            }),
            exchange = @Exchange(value = MqConstants.GROUP_EXCHANGE, type = ExchangeTypes.TOPIC,durable = "true"),
            key = MqConstants.GROUP_ROUTING_KEY
    ))
    public void handleGroupMessageBusiness(GroupMessageMQDTO groupMessageMQDTO) {
        try{
            // 保存聊天记录到ES
            EsChatMessage esChatMessage = EsChatMessage.builder()
                    .dbId(groupMessageMQDTO.getMessageId())
                    .type(SessionConstants.SESSION_TYPE_GROUP)
                    .senderId(groupMessageMQDTO.getSenderId())
                    .senderNickName(groupMessageMQDTO.getSenderNickname())
                    .targetId(groupMessageMQDTO.getGroupId())
                    .content(groupMessageMQDTO.getContent())
                    .messageType(groupMessageMQDTO.getMessageType())
                    .fileName(groupMessageMQDTO.getFileName())
                    .fileUrl(groupMessageMQDTO.getFileUrl())
                    .sendTime(groupMessageMQDTO.getSendTime())
                    .build();

            chatSearchService.saveChatMessage(esChatMessage);
            log.info("MQ消费者：群聊消息保存到ES完成，messageId：{}，群聊ID：{}",
                    groupMessageMQDTO.getMessageId(), groupMessageMQDTO.getGroupId());

            rabbitTemplate.convertAndSend(
                    MqConstants.IM_GROUP_BROADCAST_FANOUT_EXCHANGE,
                    MqConstants.IM_BROADCAST_ROUTING_KEY,
                    groupMessageMQDTO
            );

        }catch (Exception e){
            log.error("MQ消费者处理群聊消息失败，tempId：{}，发送方：{}，群聊ID：{}",
                    groupMessageMQDTO.getTempId(), groupMessageMQDTO.getSenderId(), groupMessageMQDTO.getGroupId());
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(
                    durable = "false",
                    exclusive = "true",
                    autoDelete = "true"
            ),
            exchange = @Exchange(name = MqConstants.IM_GROUP_BROADCAST_FANOUT_EXCHANGE, type = ExchangeTypes.FANOUT, durable = "true")
    ))
    public void handleGroupMessageBroadcast(GroupMessageMQDTO groupMessageMQDTO){
        try{
            //构建VO,异步推送
            GroupMessageVO groupMessageVO = GroupMessageVO.builder()
                    .messageId(groupMessageMQDTO.getMessageId())
                    .senderId(groupMessageMQDTO.getSenderId())
                    .senderNickname(groupMessageMQDTO.getSenderNickname())
                    .senderAvatar(groupMessageMQDTO.getSenderAvatar())
                    .content(groupMessageMQDTO.getContent())
                    .messageType(groupMessageMQDTO.getMessageType())
                    .fileUrl(groupMessageMQDTO.getFileUrl())
                    .fileName(groupMessageMQDTO.getFileName())
                    .fileSize(groupMessageMQDTO.getFileSize())
                    .sendTime(groupMessageMQDTO.getSendTime())
                    .tempId(groupMessageMQDTO.getTempId())
                    .groupId(groupMessageMQDTO.getGroupId())
                    .messageSeq(groupMessageMQDTO.getMessageSeq())
                    .build();

            messagingTemplate.convertAndSend("/topic/group/" + groupMessageMQDTO.getGroupId(), groupMessageVO);

            log.info("MQ消费者：群聊WebSocket推送完成，messageId：{}，单群消息序号：{}，群聊ID：{}",
                    groupMessageMQDTO.getMessageId(), groupMessageMQDTO.getMessageSeq(), groupMessageMQDTO.getGroupId());
        }catch (Exception e){
            log.warn("MQ消费者处理群聊广播消息失败(不在当前实例)，messageId：{}，群聊ID：{}",
                    groupMessageMQDTO.getMessageId(), groupMessageMQDTO.getGroupId(), e);
        }
    }

    /**
     * 处理私信撤回消息
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConstants.GROUP_RECALL_QUEUE, durable = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = MqConstants.GROUP_DLX_EXCHANGE),
                            @Argument(name = "x-dead-letter-routing-key", value = MqConstants.GROUP_DLX_ROUTING_KEY)
                    }),
            exchange = @Exchange(value = MqConstants.GROUP_RECALL_EXCHANGE, type = ExchangeTypes.TOPIC,durable = "true"),
            key = MqConstants.GROUP_RECALL_ROUTING_KEY
    ))
    public void handleGroupRecallMessage(RecallMessageDTO recallMessageDTO){
        try{
            // 删除ES中的聊天记录
            String esId = MessageConstants.ES_CHAT_MESSAGE_TYPE_GROUP + recallMessageDTO.getMessageId();
            chatSearchService.deleteChatMessage(esId);
            log.info("MQ消费者：群聊消息撤回删除ES完成，messageId：{}，群聊ID：{}",
                    recallMessageDTO.getMessageId(), recallMessageDTO.getChatId());

            rabbitTemplate.convertAndSend(
                    MqConstants.IM_GROUP_RECALL_BROADCAST_FANOUT_EXCHANGE,
                    MqConstants.IM_BROADCAST_ROUTING_KEY,
                    recallMessageDTO
            );

        }catch (Exception e){
            log.error("MQ消费者处理群聊消息撤回失败，messageId：{}，撤回用户：{}",
                    recallMessageDTO.getMessageId(), recallMessageDTO.getRecallUserId(), e);
            // 抛出异常触发MQ重试
            throw new RuntimeException("消费群聊消息撤回失败，触发重试", e);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(
                    durable = "false",
                    exclusive = "true",
                    autoDelete = "true"
            ),
            exchange = @Exchange(value = MqConstants.IM_GROUP_RECALL_BROADCAST_FANOUT_EXCHANGE, type = ExchangeTypes.FANOUT, durable = "true")
    ))
    public void handleGroupRecallMessageBroadcast(RecallMessageDTO recallMessageDTO){
        try{
            // 异步推送
            messagingTemplate.convertAndSend("/topic/group/" + recallMessageDTO.getChatId(), recallMessageDTO);
            log.info("MQ消费者：群聊消息撤回广播推送完成，messageId：{}，群聊ID：{}",
                    recallMessageDTO.getMessageId(), recallMessageDTO.getChatId());
        }catch (Exception e){
            // log.warn("MQ消费者处理群聊消息撤回广播失败(不在当前实例)，messageId：{}，群聊ID：{}",
            //        recallMessageDTO.getMessageId(), recallMessageDTO.getChatId(), e);
        }
    }

    /**
     * 处理死信队列中的群聊消息
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConstants.GROUP_DLX_QUEUE, durable = "true"),
            exchange = @Exchange(value = MqConstants.GROUP_DLX_EXCHANGE, type = ExchangeTypes.TOPIC,durable = "true"),
            key = MqConstants.GROUP_DLX_ROUTING_KEY
    ))
    public void handleGroupMessageDLX(GroupMessageMQDTO groupMessageMQDTO) {
        log.error("死信队列MQ消费者处理群聊消息失败，tempId：{}，发送方：{}，群聊ID：{}",
                groupMessageMQDTO.getTempId(), groupMessageMQDTO.getSenderId(), groupMessageMQDTO.getGroupId());
    }
}
