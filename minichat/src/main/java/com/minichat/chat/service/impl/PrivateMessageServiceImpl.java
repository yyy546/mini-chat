package com.minichat.chat.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minichat.chat.dto.PrivateMessageDTO;
import com.minichat.chat.dto.PrivateMessageMQDTO;
import com.minichat.chat.dto.RecallMessageDTO;
import com.minichat.chat.vo.FileVO;
import com.minichat.common.constants.MqConstants;
import com.minichat.common.constants.MessageConstants;
import com.minichat.common.constants.OssConstants;
import com.minichat.common.exception.BusinessException;
import com.minichat.common.exception.ForbiddenException;
import com.minichat.common.exception.MessageException;
import com.minichat.common.exception.NotFoundException;
import com.minichat.common.exception.SystemException;
import com.minichat.common.exception.ValidationException;
import com.minichat.chat.entity.PrivateMessage;
import com.minichat.user.entity.User;
import com.minichat.friend.mapper.FriendMapper;
import com.minichat.chat.mapper.PrivateMessageMapper;
import com.minichat.user.mapper.UserMapper;
import com.minichat.chat.service.PrivateMessageService;
import com.minichat.chat.vo.PrivateMessageVO;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrivateMessageServiceImpl extends AbstractMessageService implements PrivateMessageService {

    private final UserMapper userMapper;
    private final PrivateMessageMapper privateMessageMapper;
    private final FriendMapper friendMapper;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public String sendPrivateMessage(PrivateMessageDTO privateMessageDTO, Principal principal) {
        try {
            Long realSenderId = Long.parseLong(principal.getName());
            privateMessageDTO.setSenderId(realSenderId);

            if (MessageConstants.TEXT.equals(privateMessageDTO.getMessageType())) {
                if (StringUtils.isEmpty(privateMessageDTO.getContent()) || privateMessageDTO.getContent().length() > 2000) {
                    throw new ValidationException("文本消息内容为空或超过2000字");
                }
            } else {
                String fileUrl = privateMessageDTO.getFileUrl();
                String fileName = privateMessageDTO.getFileName();
                Long fileSize = privateMessageDTO.getFileSize();
                boolean valid = validateFile(fileUrl, fileName, fileSize);
                if(!valid){
                    throw new ValidationException("文件信息不合法");
                }
            }

            User sender = userMapper.selectById(realSenderId);
            User receiver = userMapper.selectById(privateMessageDTO.getReceiverId());

            if (sender == null) {
                throw new NotFoundException("发送方不存在");
            }
            if (receiver == null) {
                throw new NotFoundException("接收方不存在");
            }

            int senderFriendCount = friendMapper.selectFriendByUserIdAndFriendId(realSenderId, privateMessageDTO.getReceiverId());
            int receiverFriendCount = friendMapper.selectFriendByUserIdAndFriendId(privateMessageDTO.getReceiverId(), realSenderId);

            if (senderFriendCount == 0 || receiverFriendCount == 0) {
                log.warn("用户 {} 尝试向已删除的好友 {} 发送消息（发送方好友计数: {}, 接收方好友计数: {}）",
                        realSenderId,
                        privateMessageDTO.getReceiverId(),
                        senderFriendCount,
                        receiverFriendCount);

                throw new ValidationException("发送失败,请先添加对方为好友");
            }

            String content = privateMessageDTO.getContent();
            if (!MessageConstants.TEXT.equals(privateMessageDTO.getMessageType()) && StringUtils.isEmpty(content)) {
                content = MessageConstants.IMAGE.equals(privateMessageDTO.getMessageType()) ? "[图片]" : "[文件]";
            }

            PrivateMessage privateMessage = PrivateMessage.builder()
                    .senderId(realSenderId)
                    .receiverId(privateMessageDTO.getReceiverId())
                    .messageType(privateMessageDTO.getMessageType())
                    .content(content)
                    .fileUrl(privateMessageDTO.getFileUrl())
                    .fileName(privateMessageDTO.getFileName())
                    .fileSize(privateMessageDTO.getFileSize())
                    .isRead(MessageConstants.NOT_READ)
                    .isRecall(MessageConstants.NOT_RECALL)
                    .sendTime(LocalDateTime.now())
                    .build();

            privateMessageMapper.insert(privateMessage);
            Long messageId = privateMessage.getId();
            log.info("消息保存到数据库成功，messageId：{}", messageId);

            PrivateMessageMQDTO privateMessageMQDTO = PrivateMessageMQDTO.builder()
                    .messageId(messageId)
                    .senderId(realSenderId)
                    .senderNickname(sender.getNickname())
                    .senderAvatar(sender.getAvatar())
                    .receiverId(privateMessageDTO.getReceiverId())
                    .receiverNickname(receiver.getNickname())
                    .messageType(privateMessageDTO.getMessageType())
                    .content(content)
                    .fileUrl(privateMessageDTO.getFileUrl())
                    .fileName(privateMessageDTO.getFileName())
                    .fileSize(privateMessageDTO.getFileSize())
                    .tempId(privateMessageDTO.getTempId())
                    .sendTime(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(MqConstants.PRIVATE_EXCHANGE,
                    MqConstants.PRIVATE_ROUTING_KEY,
                    privateMessageMQDTO);

            log.info("消息已发送到MQ队列，tempId: {}, 发送方ID: {}, 接收方ID: {}",
                    privateMessageDTO.getTempId(),
                    realSenderId,
                    privateMessageDTO.getReceiverId());

            return "消息发送成功";

        } catch (NumberFormatException e) {
            throw new SystemException("用户身份解析失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("私信推送到MQ失败", e);
            throw new MessageException("消息发送失败：服务器内部错误");
        }
    }

    @Override
    public IPage<PrivateMessageVO> getPrivateMessageHistory(Long currentUserId, Long targetUserId, Integer page, Integer pageSize) {
        Page<PrivateMessage> pageParam = new Page<>(page != null ? page : 1, pageSize != null ? pageSize : 50);

        IPage<PrivateMessage> entityPage = privateMessageMapper.selectHistoryByTwoUsers(pageParam, currentUserId, targetUserId);

        List<PrivateMessage> messageList = entityPage.getRecords();

        if(entityPage.getRecords().isEmpty()){
            return new Page<>();
        }

        Set<Long> userIds = new HashSet<>();
        messageList.forEach(message -> {
            userIds.add(message.getSenderId());
            userIds.add(message.getReceiverId());
        });

        List<User> userList = userMapper.selectBatchIds(userIds);

        Map<Long, User> userMap = userList.stream().collect(Collectors.toMap(User::getId, user -> user));

        List<PrivateMessageVO> privateMessageVOList = messageList.stream().map(message -> PrivateMessageVO.builder()
                .messageId(message.getId())
                .senderId(message.getSenderId())
                .senderNickname(userMap.get(message.getSenderId()).getNickname())
                .senderAvatar(userMap.get(message.getSenderId()).getAvatar())
                .receiverId(message.getReceiverId())
                .receiverNickname(userMap.get(message.getReceiverId()).getNickname())
                .content(Objects.equals(message.getIsRecall(), MessageConstants.RECALL) ? MessageConstants.DEFAULT_RECALL_MESSAGE : message.getContent())
                .messageType(Objects.equals(message.getIsRecall(), MessageConstants.RECALL) ? MessageConstants.RECALL_MESSAGE : message.getMessageType())
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .isRead(message.getIsRead())
                .sendTime(message.getSendTime())
                .build()).collect(Collectors.toList());

        Page<PrivateMessageVO> resultPage = new Page<>();
        resultPage.setCurrent(entityPage.getCurrent());
        resultPage.setSize(entityPage.getSize());
        resultPage.setTotal(entityPage.getTotal());
        resultPage.setRecords(privateMessageVOList);

        return resultPage;
    }

    @Override
    public void markMessagesAsRead(Long currentUserId, Long receiverId) {
        if (receiverId == null || receiverId <= 0) {
            throw new ValidationException("聊天对象ID不能为空");
        }
        privateMessageMapper.updateIsRead(currentUserId, receiverId);
    }

    @Override
    public FileVO uploadPrivateFile(MultipartFile file, Integer type) {
        if (!(MessageConstants.IMAGE.equals(type) || MessageConstants.FILE.equals(type))) {
            throw new ValidationException("文件类型不合法（2=图片，3=文件）");
        }
        if (file.isEmpty()) {
            throw new ValidationException("上传文件不能为空");
        }

        return uploadFileCommon(file, OssConstants.PRIVATE_IMAGE_PATH, OssConstants.PRIVATE_FILE_PATH);
    }

    @Override
    public void recallPrivateMessage(Long currentUserId, Long messageId) {
        PrivateMessage privateMessage = privateMessageMapper.selectById(messageId);
        if(privateMessage == null){
            throw new NotFoundException("消息不存在");
        }
        if (!privateMessage.getSenderId().equals(currentUserId)) {
            throw new ForbiddenException("只有发送者才能撤回消息");
        }
        LocalDateTime sendTime = privateMessage.getSendTime();
        LocalDateTime now = LocalDateTime.now();

        Boolean isRecallSuccess = processRecallMessage(sendTime, privateMessage, msg -> {
            msg.setIsRecall(MessageConstants.RECALL);
            msg.setRecallTime(now);
            privateMessageMapper.updateIsRecall(msg);
        });

        if (!isRecallSuccess) {
            throw new ValidationException("消息已发送超过撤回时间限制，无法撤回");
        }

        RecallMessageDTO recallMessageDTO = RecallMessageDTO.builder()
                .messageId(messageId)
                .chatId(privateMessage.getReceiverId())
                .recallUserId(currentUserId)
                .isGroup(false)
                .timestamp(now.toEpochSecond(ZoneOffset.of("+8")))
                .build();

        rabbitTemplate.convertAndSend(MqConstants.PRIVATE_RECALL_EXCHANGE,
                MqConstants.PRIVATE_RECALL_ROUTING_KEY,
                recallMessageDTO);
    }

}
