package com.minichat.chat.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minichat.chat.dto.GroupMessageDTO;
import com.minichat.chat.dto.GroupMessageMQDTO;
import com.minichat.chat.dto.RecallMessageDTO;
import com.minichat.chat.vo.FileVO;
import com.minichat.chat.vo.GroupMessageVO;
import com.minichat.chat.entity.GroupMessage;
import com.minichat.common.constants.MessageConstants;
import com.minichat.common.constants.OssConstants;
import com.minichat.common.constants.RedisConstants;
import com.minichat.common.result.Result;
import com.minichat.chat.service.GroupMessageService;

import java.security.Principal;


import com.minichat.common.constants.MqConstants;
import com.minichat.group.entity.ChatGroup;
import com.minichat.group.mapper.ChatGroupMapper;
import com.minichat.group.mapper.GroupMemberMapper;
import com.minichat.chat.mapper.GroupMessageMapper;
import com.minichat.user.mapper.UserMapper;
import com.minichat.common.util.UserContext;
import com.minichat.user.entity.User;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    public Result<String> sendGroupMessage(GroupMessageDTO groupMessageDTO, Principal principal) {
        try {
            Long realSenderId = Long.parseLong(principal.getName());
            groupMessageDTO.setSenderId(realSenderId);

            // 不同类型消息的特有校验
            if (groupMessageDTO.getMessageType() == 1) {
                // 文本消息：校验内容
                if (StringUtils.isEmpty(groupMessageDTO.getContent()) || groupMessageDTO.getContent().length() > 2000) {
                    return Result.error("文本消息内容为空或超过2000字");
                }
            } else {
                String fileUrl = groupMessageDTO.getFileUrl();
                String fileName = groupMessageDTO.getFileName();
                Long fileSize = groupMessageDTO.getFileSize();
                boolean valid = validateFile(fileUrl, fileName, fileSize);
                if(!valid){
                    return Result.error("文件信息不合法");
                }
            }

            User sender = userMapper.selectById(realSenderId);
            ChatGroup group = chatGroupMapper.selectById(groupMessageDTO.getGroupId());

            if (sender == null) {
                return Result.error("发送方不存在");
            }
            if (group == null) {
                return Result.error("群聊不存在");
            }

            // 检查用户是否在群聊中且未被禁言
            int groupMemberCount = groupMemberMapper.selectByGroupIdAndUserId(groupMessageDTO.getGroupId(), realSenderId);

            if(groupMemberCount == 0){
                return Result.error("用户不在群聊中或已被禁言，无法发送消息");
            }

            // 对于文件/图片消息，如果content为空，设置默认值
            String content = groupMessageDTO.getContent();
            if (groupMessageDTO.getMessageType() != 1 && StringUtils.isEmpty(content)) {
                content = groupMessageDTO.getMessageType() == 2 ? "[图片]" : "[文件]";
            }

            //  存储到数据库
            //实现单群消息的有序存储
            Long groupId = groupMessageDTO.getGroupId();
            Long messageSeq = redisTemplate.opsForValue().
                    increment(RedisConstants.GROUP_MESSAGE_SEQ_KEY_PREFIX + groupId, 1);

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

            //构建MQDTO
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

            //  发送到MQ队列
            rabbitTemplate.convertAndSend(MqConstants.GROUP_EXCHANGE,
                    MqConstants.GROUP_ROUTING_KEY,
                    groupMessageMQDTO);

            log.info("群聊消息已发送到MQ队列，tempId: {}, 发送方ID: {}, 接收方ID: {}",
                    groupMessageDTO.getTempId(),
                    realSenderId,
                    groupMessageDTO.getGroupId());

            //直接返回成功结果，不等待MQ处理
            return Result.success("消息发送成功");

        } catch (Exception e){
            log.error("发送群聊消息异常", e);
            return Result.error("发送群聊消息失败");
        }
    }

    @Override
    public Result<IPage<GroupMessageVO>> getGroupMessageHistory(Long groupId, Integer page, Integer pageSize) {

        // 默认第一页，每页50条
        Page<GroupMessageVO> pageParam = new Page<>(page != null ? page : 1, pageSize != null ? pageSize : 50);
        // 查询群聊消息历史记录
        IPage<GroupMessageVO> messagePage = groupMessageMapper.selectByGroupId(pageParam, groupId);

        // 处理撤回消息
        messagePage.getRecords().forEach(message -> {
            if (Objects.equals(message.getIsRecall(), MessageConstants.RECALL)) {
                message.setContent(MessageConstants.DEFAULT_RECALL_MESSAGE);
                message.setMessageType(MessageConstants.RECALL_MESSAGE);
            }
        });

        // 返回结果
        return Result.success(messagePage);
    }

    @Override
    public Result<FileVO> uploadGroupFile(MultipartFile file, Integer type) {
        // 校验文件类型（前端传入的type仅作为参考，后端会根据实际文件类型自动判断）
        if (!(MessageConstants.IMAGE.equals(type) || MessageConstants.FILE.equals(type))) {
            return Result.error("文件类型不合法（2=图片，3=文件）");
        }
        // 校验文件是否为空
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        return uploadFileCommon(file, OssConstants.GROUP_IMAGE_PATH, OssConstants.GROUP_FILE_PATH);
    }

    @Override
    public Result<String> markGroupMessageRead(Long groupId) {
        Long currentUserId = UserContext.getCurUserId();

        Object seqObj = redisTemplate.opsForValue().get(RedisConstants.GROUP_MESSAGE_SEQ_KEY_PREFIX + groupId);
        Long messageSeq = seqObj != null ? Long.valueOf(seqObj.toString()) : null;

        if (messageSeq == null) {
            return Result.success("群聊消息不存在");
        }

        groupMemberMapper.updateLastReadMessageId(groupId, currentUserId, messageSeq);

        return Result.success("群聊消息已标记为已读");
    }

    @Override
    public Result<String> recallGroupMessage(Long groupId, Long messageId) {
        // 校验消息是否存在
        GroupMessage groupMessage = groupMessageMapper.selectById(messageId);
        if (groupMessage == null) {
            return Result.error("消息不存在");
        }

        // 校验是否是发送者
        Long senderId = groupMessage.getSenderId();
        Long currentUserId = UserContext.getCurUserId();
        if (!senderId.equals(currentUserId)) {
            return Result.error("只有发送者才能撤回消息");
        }

        LocalDateTime sendTime = groupMessage.getSendTime();
        LocalDateTime now = LocalDateTime.now();

        Boolean isRecallSuccess = processRecallMessage(sendTime, groupMessage, msg -> {
            msg.setIsRecall(MessageConstants.RECALL);
            msg.setRecallTime(now);
            groupMessageMapper.updateIsRecall(msg);
        });

        if (!isRecallSuccess) {
            return Result.error("消息已发送超过撤回时间限制，无法撤回");
        }

        // 构建撤回消息DTO
        RecallMessageDTO recallMessageDTO = RecallMessageDTO.builder()
                .messageId(messageId)
                .chatId(groupId)
                .recallUserId(currentUserId)
                .isGroup(true)
                .timestamp(now.toEpochSecond(ZoneOffset.of("+8")))
                .build();

        // 发送撤回消息到MQ
        rabbitTemplate.convertAndSend(MqConstants.GROUP_RECALL_EXCHANGE,
                MqConstants.GROUP_RECALL_ROUTING_KEY,
                recallMessageDTO);

        return Result.success("消息已撤回");
    }
}

