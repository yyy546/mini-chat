package com.minichat.chat.listener;

import com.minichat.chat.dto.PrivateMessageMQDTO;
import com.minichat.chat.dto.RecallMessageDTO;
import com.minichat.chat.entity.EsChatMessage;
import com.minichat.chat.service.ChatSearchService;
import com.minichat.chat.vo.PrivateMessageVO;
import com.minichat.common.mq.MqConstants;
import com.minichat.common.core.constants.MessageConstants;
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
public class PrivateMessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatSearchService chatSearchService;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 处理私信消息业务
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstants.PRIVATE_QUEUE, durable = "true",
            arguments = {
                    @Argument(name = "x-dead-letter-exchange", value = MqConstants.PRIVATE_DLX_EXCHANGE),
                    @Argument(name = "x-dead-letter-routing-key", value = MqConstants.PRIVATE_DLX_ROUTING_KEY)
            }),
            exchange = @Exchange(name = MqConstants.PRIVATE_EXCHANGE, type = ExchangeTypes.DIRECT, durable = "true"),
            key = MqConstants.PRIVATE_ROUTING_KEY))
    public void handlePrivateMessageBusiness(PrivateMessageMQDTO privateMessageMQDTO){
        try{
             // 保存聊天记录到ES
            EsChatMessage esChatMessage = EsChatMessage.builder()
                    .dbId(privateMessageMQDTO.getMessageId())
                    .type(SessionConstants.SESSION_TYPE_PRIVATE)
                    .senderId(privateMessageMQDTO.getSenderId())
                    .senderNickName(privateMessageMQDTO.getSenderNickname())
                    .targetId(privateMessageMQDTO.getReceiverId())
                    .content(privateMessageMQDTO.getContent())
                    .messageType(privateMessageMQDTO.getMessageType())
                    .fileName(privateMessageMQDTO.getFileName())
                    .fileUrl(privateMessageMQDTO.getFileUrl())
                    .sendTime(privateMessageMQDTO.getSendTime())
                    .build();

            chatSearchService.saveChatMessage(esChatMessage);
            log.info("MQ业务处理：私信已存ES，messageId：{}", privateMessageMQDTO.getMessageId());

            rabbitTemplate.convertAndSend(
                    MqConstants.IM_PRIVATE_BROADCAST_FANOUT_EXCHANGE,
                    MqConstants.IM_BROADCAST_ROUTING_KEY,
                    privateMessageMQDTO
            );

            log.info("MQ业务处理：转发私信到Fanout交换机，准备广播推送，messageId：{}", privateMessageMQDTO.getMessageId());

        }catch (Exception e){
            log.error("MQ消费者处理私信失败，tempId：{}，发送方：{}，接收方：{}",
                    privateMessageMQDTO.getTempId(), privateMessageMQDTO.getSenderId(), privateMessageMQDTO.getReceiverId(), e);
            // 抛出异常触发MQ重试
            throw new RuntimeException("消费私信失败，触发重试", e);
        }
    }
    /**
     * 处理私信广播消息
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(
                    durable = "false",
                    exclusive = "true",
                    autoDelete = "true"
            ),
            exchange = @Exchange(name = MqConstants.IM_PRIVATE_BROADCAST_FANOUT_EXCHANGE, type = ExchangeTypes.FANOUT, durable = "true")
    ))
    public void handlePrivateMessageBroadcast(PrivateMessageMQDTO privateMessageMQDTO){
        try{
            //  构建VO,异步推送
            PrivateMessageVO privateMessageVO = PrivateMessageVO.builder()
                    .messageId(privateMessageMQDTO.getMessageId())
                    .senderId(privateMessageMQDTO.getSenderId())
                    .senderNickname(privateMessageMQDTO.getSenderNickname())
                    .senderAvatar(privateMessageMQDTO.getSenderAvatar())
                    .content(privateMessageMQDTO.getContent())
                    .messageType(privateMessageMQDTO.getMessageType())
                    .fileUrl(privateMessageMQDTO.getFileUrl())
                    .fileName(privateMessageMQDTO.getFileName())
                    .fileSize(privateMessageMQDTO.getFileSize())
                    .sendTime(privateMessageMQDTO.getSendTime())
                    .isRead(MessageConstants.NOT_READ)
                    .tempId(privateMessageMQDTO.getTempId())
                    .receiverId(privateMessageMQDTO.getReceiverId())
                    .receiverNickname(privateMessageMQDTO.getReceiverNickname())
                    .build();

            // 推送消息给接收方
            messagingTemplate.convertAndSendToUser(
                    privateMessageMQDTO.getReceiverId().toString(),
                    "/queue/private",
                    privateMessageVO
            );

            // 推送消息给发送方（用于前端回显）
            messagingTemplate.convertAndSendToUser(
                    privateMessageMQDTO.getSenderId().toString(),
                    "/queue/private",
                    privateMessageVO
            );

            log.info("MQ广播推送：检查当前实例连接，messageId：{}，接收方：{}",
                    privateMessageMQDTO.getMessageId(), privateMessageMQDTO.getReceiverId());

        }catch (Exception e){
            // 仅打印日志，不抛异常（避免触发MQ重试，其他实例可能推送成功）
            // log.warn("MQ广播推送私信失败（用户不在当前实例），messageId：{}", privateMessageMQDTO.getMessageId(), e);
        }
    }

    /**
     * 处理私信撤回消息业务
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstants.PRIVATE_RECALL_QUEUE, durable = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = MqConstants.PRIVATE_DLX_EXCHANGE),
                            @Argument(name = "x-dead-letter-routing-key", value = MqConstants.PRIVATE_DLX_ROUTING_KEY)
                    }),
            exchange = @Exchange(name = MqConstants.PRIVATE_RECALL_EXCHANGE, type = ExchangeTypes.DIRECT, durable = "true"),
            key = MqConstants.PRIVATE_RECALL_ROUTING_KEY))
    public void handlePrivateRecallMessageBusiness(RecallMessageDTO recallMessageDTO){
        try{
            // 删除ES中的聊天记录
            String esId = MessageConstants.ES_CHAT_MESSAGE_TYPE_PRIVATE + recallMessageDTO.getMessageId();
            chatSearchService.deleteChatMessage(esId);
            log.info("MQ广播推送：删除ES聊天记录，messageId：{}，esId：{}",
                    recallMessageDTO.getMessageId(), esId);

            rabbitTemplate.convertAndSend(
                    MqConstants.IM_PRIVATE_RECALL_BROADCAST_FANOUT_EXCHANGE,
                    MqConstants.IM_BROADCAST_ROUTING_KEY,
                    recallMessageDTO
            );

        }catch (Exception e){
            log.error("MQ消费者处理私信撤回失败，messageId：{}，撤回用户：{}",
                    recallMessageDTO.getMessageId(), recallMessageDTO.getRecallUserId(), e);
            // 抛出异常触发MQ重试
            throw new RuntimeException("消费私信撤回失败，触发重试", e);
        }
    }

    /**
     * 处理私信撤回消息广播
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(
                    durable = "false",
                    exclusive = "true",
                    autoDelete = "true"
            ),
            exchange = @Exchange(name = MqConstants.IM_PRIVATE_RECALL_BROADCAST_FANOUT_EXCHANGE, type = ExchangeTypes.FANOUT, durable = "true")
    ))
    public void handlePrivateRecallMessageBroadcast(RecallMessageDTO recallMessageDTO){
        try{
            // 推送撤回消息给接收方
            messagingTemplate.convertAndSendToUser(
                    recallMessageDTO.getChatId().toString(),
                    "/queue/private_recall",
                    recallMessageDTO
            );

            // 推送撤回消息给撤回方
            messagingTemplate.convertAndSendToUser(
                    recallMessageDTO.getRecallUserId().toString(),
                    "/queue/private_recall",
                    recallMessageDTO
            );

            log.info("MQ广播推送：检查当前实例连接，messageId：{}，接收方：{}",
                    recallMessageDTO.getMessageId(), recallMessageDTO.getChatId());
        }catch (Exception e){
            // log.warn("MQ广播推送私信撤回失败（用户不在当前实例），messageId：{}", recallMessageDTO.getMessageId(), e);
        }
    }

    /**
     * 处理私信死信队列消息
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstants.PRIVATE_DLX_QUEUE, durable = "true"),
            exchange = @Exchange(name = MqConstants.PRIVATE_DLX_EXCHANGE, type = ExchangeTypes.DIRECT, durable = "true"),
            key = MqConstants.PRIVATE_DLX_ROUTING_KEY))
    public void handlePrivateMessageDLX(PrivateMessageMQDTO privateMessageMQDTO){
        log.error("死信队列MQ消费者处理私信失败，tempId：{}，发送方：{}，接收方：{}",
                privateMessageMQDTO.getTempId(), privateMessageMQDTO.getSenderId(), privateMessageMQDTO.getReceiverId());
    }

}
