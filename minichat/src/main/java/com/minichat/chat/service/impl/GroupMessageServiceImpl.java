package com.minichat.chat.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minichat.chat.dto.GroupMessageDTO;
import com.minichat.chat.dto.GroupMessageMQDTO;
import com.minichat.chat.dto.RecallMessageDTO;
import com.minichat.chat.entity.GroupMessage;
import com.minichat.chat.mapper.GroupMessageMapper;
import com.minichat.chat.service.GroupMessageService;
import com.minichat.chat.vo.FileVO;
import com.minichat.chat.vo.GroupMessageVO;
import com.minichat.common.constants.MessageConstants;
import com.minichat.common.constants.MqConstants;
import com.minichat.common.constants.OssConstants;
import com.minichat.common.constants.RedisConstants;
import com.minichat.common.exception.ChatException;
import com.minichat.common.exception.ErrorCode;
import com.minichat.common.util.UserContext;
import com.minichat.group.entity.ChatGroup;
import com.minichat.group.mapper.ChatGroupMapper;
import com.minichat.group.mapper.GroupMemberMapper;
import com.minichat.user.entity.User;
import com.minichat.user.mapper.UserMapper;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupMessageServiceImpl extends AbstractMessageService implements GroupMessageService {

    private final UserMapper userMapper;
    private final ChatGroupMapper chatGroupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void sendGroupMessage(GroupMessageDTO groupMessageDTO, Principal principal) {
        Long realSenderId = Long.parseLong(principal.getName());
        groupMessageDTO.setSenderId(realSenderId);

        if (groupMessageDTO.getMessageType() == 1) {
            if (StringUtils.isEmpty(groupMessageDTO.getContent()) || groupMessageDTO.getContent().length() > 2000) {
                throw new ChatException(ErrorCode.MESSAGE_TOO_LONG, "文本消息内容为空或超过2000字");
            }
        } else {
            String fileUrl = groupMessageDTO.getFileUrl();
            String fileName = groupMessageDTO.getFileName();
            Long fileSize = groupMessageDTO.getFileSize();
            if (!validateFile(fileUrl, fileName, fileSize)) {
                throw new ChatException(ErrorCode.MESSAGE_FILE_INVALID, "文件信息不合法");
            }
        }

        User sender = userMapper.selectById(realSenderId);
        ChatGroup group = chatGroupMapper.selectById(groupMessageDTO.getGroupId());

        if (sender == null) {
            throw new ChatException(ErrorCode.BAD_REQUEST, "发送方不存在");
        }
        if (group == null) {
            throw new ChatException(ErrorCode.GROUP_NOT_FOUND, "群聊不存在");
        }

        int groupMemberCount = groupMemberMapper.selectByGroupIdAndUserId(groupMessageDTO.getGroupId(), realSenderId);

        if (groupMemberCount == 0) {
            throw new ChatException(ErrorCode.NOT_GROUP_MEMBER, "用户不在群聊中或已被禁言，无法发送消息");
        }

        String content = groupMessageDTO.getContent();
        if (groupMessageDTO.getMessageType() != 1 && StringUtils.isEmpty(content)) {
            content = groupMessageDTO.getMessageType() == 2 ? "[图片]" : "[文件]";
        }

        Long groupId = groupMessageDTO.getGroupId();
        Long messageSeq = redisTemplate.opsForValue()
                .increment(RedisConstants.GROUP_MESSAGE_SEQ_KEY_PREFIX + groupId, 1);

        GroupMessage groupMessage = GroupMessage.builder()
                .messageSeq(messageSeq)
                .groupId(groupMessageDTO.getGroupId())
                .senderId(realSenderId)
                .messageType(groupMessageDTO.getMessageType())
                .content(content)
                .fileUrl(groupMessageDTO.getFileUrl())
                .fileName(groupMessageDTO.getFileName())
                .fileSize(groupMessageDTO.getFileSize())
                .isRecall(MessageConstants.NOT_RECALL)
                .sendTime(LocalDateTime.now())
                .build();

        groupMessageMapper.insert(groupMessage);
        Long messageId = groupMessage.getId();
        log.info("群聊消息保存到数据库成功，messageId：{}", messageId);

        GroupMessageMQDTO groupMessageMQDTO = GroupMessageMQDTO.builder()
                .messageId(messageId)
                .messageSeq(messageSeq)
                .groupId(groupId)
                .senderId(realSenderId)
                .senderNickname(sender.getNickname())
                .senderAvatar(sender.getAvatar())
                .messageType(groupMessageDTO.getMessageType())
                .content(content)
                .fileUrl(groupMessageDTO.getFileUrl())
                .fileName(groupMessageDTO.getFileName())
                .fileSize(groupMessageDTO.getFileSize())
                .tempId(groupMessageDTO.getTempId())
                .sendTime(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(MqConstants.GROUP_EXCHANGE,
                MqConstants.GROUP_ROUTING_KEY,
                groupMessageMQDTO);

        log.info("群聊消息已发送到MQ队列，tempId: {}, 发送方ID: {}, 接收方ID: {}",
                groupMessageDTO.getTempId(),
                realSenderId,
                groupMessageDTO.getGroupId());
    }

    @Override
    public IPage<GroupMessageVO> getGroupMessageHistory(Long groupId, Integer page, Integer pageSize) {

        Page<GroupMessageVO> pageParam = new Page<>(page != null ? page : 1, pageSize != null ? pageSize : 50);
        IPage<GroupMessageVO> messagePage = groupMessageMapper.selectByGroupId(pageParam, groupId);

        messagePage.getRecords().forEach(message -> {
            if (Objects.equals(message.getIsRecall(), MessageConstants.RECALL)) {
                message.setContent(MessageConstants.DEFAULT_RECALL_MESSAGE);
                message.setMessageType(MessageConstants.RECALL_MESSAGE);
            }
        });

        return messagePage;
    }

    @Override
    public FileVO uploadGroupFile(MultipartFile file, Integer type) {
        if (!(MessageConstants.IMAGE.equals(type) || MessageConstants.FILE.equals(type))) {
            throw new ChatException(ErrorCode.MESSAGE_FILE_INVALID, "文件类型不合法（2=图片，3=文件）");
        }
        if (file.isEmpty()) {
            throw new ChatException(ErrorCode.BAD_REQUEST, "上传文件不能为空");
        }

        return uploadFileCommon(file, OssConstants.GROUP_IMAGE_PATH, OssConstants.GROUP_FILE_PATH);
    }

    @Override
    public void markGroupMessageRead(Long groupId) {
        Long currentUserId = UserContext.getCurUserId();

        Object seqObj = redisTemplate.opsForValue().get(RedisConstants.GROUP_MESSAGE_SEQ_KEY_PREFIX + groupId);
        Long messageSeq = seqObj != null ? Long.valueOf(seqObj.toString()) : null;

        if (messageSeq == null) {
            return;
        }

        groupMemberMapper.updateLastReadMessageId(groupId, currentUserId, messageSeq);
    }

    @Override
    public void recallGroupMessage(Long groupId, Long messageId) {
        GroupMessage groupMessage = groupMessageMapper.selectById(messageId);
        if (groupMessage == null) {
            throw new ChatException(ErrorCode.MESSAGE_NOT_FOUND, "消息不存在");
        }

        Long senderId = groupMessage.getSenderId();
        Long currentUserId = UserContext.getCurUserId();
        if (!senderId.equals(currentUserId)) {
            throw new ChatException(ErrorCode.FORBIDDEN, "只有发送者才能撤回消息");
        }

        LocalDateTime sendTime = groupMessage.getSendTime();
        LocalDateTime now = LocalDateTime.now();

        Boolean isRecallSuccess = processRecallMessage(sendTime, groupMessage, msg -> {
            msg.setIsRecall(MessageConstants.RECALL);
            msg.setRecallTime(now);
            groupMessageMapper.updateIsRecall(msg);
        });

        if (!isRecallSuccess) {
            throw new ChatException(ErrorCode.MESSAGE_RECALL_TIMEOUT, "消息已发送超过撤回时间限制，无法撤回");
        }

        RecallMessageDTO recallMessageDTO = RecallMessageDTO.builder()
                .messageId(messageId)
                .chatId(groupId)
                .recallUserId(currentUserId)
                .isGroup(true)
                .timestamp(now.toEpochSecond(ZoneOffset.of("+8")))
                .build();

        rabbitTemplate.convertAndSend(MqConstants.GROUP_RECALL_EXCHANGE,
                MqConstants.GROUP_RECALL_ROUTING_KEY,
                recallMessageDTO);
    }
}
