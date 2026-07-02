package com.minichat.session.service.impl;

import com.minichat.friend.service.FriendService;
import com.minichat.friend.vo.FriendVO;
import com.minichat.group.service.GroupService;
import com.minichat.group.vo.GroupVO;
import com.minichat.session.vo.SessionVO;
import com.minichat.common.cache.CacheKeys;
import com.minichat.common.constants.SessionConstants;
import com.minichat.group.dto.GroupMemberLastReadDTO;
import com.minichat.chat.dto.MessageUnreadCountDTO;
import com.minichat.chat.entity.GroupMessage;
import com.minichat.chat.entity.PrivateMessage;
import com.minichat.group.mapper.GroupMemberMapper;
import com.minichat.chat.mapper.GroupMessageMapper;
import com.minichat.chat.mapper.PrivateMessageMapper;
import com.minichat.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final FriendService friendService;
    private final GroupService groupService;
    private final GroupMemberMapper groupMemberMapper;
    private final PrivateMessageMapper privateMessageMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<SessionVO> getSessionList(Long currentUserId) {
        List<SessionVO> sessionVOList = new ArrayList<>();

        List<FriendVO> friendList = friendService.getFriendList(currentUserId);
        if(!friendList.isEmpty()){
            // 获取所有好友的id
            List<Long> friendIds = friendList.stream().map(FriendVO::getFriendId).toList();
            //获取所有好友的未读消息数量
            List<MessageUnreadCountDTO> unreadCounts = privateMessageMapper.selectUnreadCountBatch(currentUserId, friendIds);
            Map<Long, Long> unreadCountMap = unreadCounts.stream()
                    .collect(Collectors.toMap(
                            MessageUnreadCountDTO::getFriendId,
                            MessageUnreadCountDTO::getCount
                    ));
            //获取所有好友的最后一条消息
            List<PrivateMessage> lastMessages = privateMessageMapper.selectLastMessageBatch(currentUserId, friendIds);
            Map<Long, PrivateMessage> lastMessageMap = new HashMap<>();
            lastMessages.forEach(privateMessage -> {
                Long friendId = privateMessage.getSenderId().equals(currentUserId) ? privateMessage.getReceiverId() : privateMessage.getSenderId();
                lastMessageMap.put(friendId, privateMessage);
            });
            // 构建会话VO
            for (FriendVO friend : friendList) {
                PrivateMessage lastMessage = lastMessageMap.get(friend.getFriendId());
                Long unreadCount = unreadCountMap.getOrDefault(friend.getFriendId(), 0L);
                // 备注名不为空时, 优先使用备注名
                String name = friend.getRemarkName() != null ? friend.getRemarkName() : friend.getNickname();

                SessionVO sessionVO = SessionVO.builder()
                        .id(friend.getFriendId())
                        .type(SessionConstants.SESSION_TYPE_PRIVATE)
                        .name(name)
                        .avatar(friend.getAvatar())
                        .lastMessageTime(lastMessage != null ? lastMessage.getSendTime() : null)
                        .unreadCount(unreadCount)
                        .build();
                sessionVOList.add(sessionVO);
            }
        }

        List<GroupVO> groupVOList = groupService.getGroupList(currentUserId);
        if(!groupVOList.isEmpty()){
            // 提取所有 groupId
            List<Long> groupIds = groupVOList.stream().map(GroupVO::getId).toList();

            //  【批量查询】群最后一条消息 -> Map<GroupId, GroupMessage>
            List<GroupMessage> groupLastMsgs = groupMessageMapper.selectLastMessageBatch(groupIds);
            Map<Long, GroupMessage> groupMsgMap = groupLastMsgs.stream()
                    .collect(Collectors.toMap(GroupMessage::getGroupId, java.util.function.Function.identity()));

            // 【批量查询】用户在群里的最后阅读位置 -> Map<GroupId, LastReadId>
            List<GroupMemberLastReadDTO> readPosList = groupMemberMapper.selectLastReadBatch(currentUserId, groupIds);
            Map<Long, Long> readPosMap = readPosList.stream().collect(Collectors.toMap(
                    GroupMemberLastReadDTO::getGroupId,
                    dto -> dto.getLastReadMessageId() != null ? dto.getLastReadMessageId() : 0L
            ));

            // 【Redis 批量优化】一次性获取所有群的当前最新 Seq
            List<String> redisKeys = groupIds.stream()
                    .map(id -> CacheKeys.groupMessageSeq(id))
                    .toList();
            List<Object> currentSeqs = redisTemplate.opsForValue().multiGet(redisKeys); // 使用 multiGet

            // 【内存组装】
            for (int i = 0; i < groupVOList.size(); i++) {
                GroupVO group = groupVOList.get(i);
                GroupMessage lastMsg = groupMsgMap.get(group.getId());
                Long lastReadSeq = readPosMap.getOrDefault(group.getId(), 0L);

                // 解析 Redis 结果
                long nowMessageSeq = 0L;
                if(currentSeqs != null){
                    Object seqObj = currentSeqs.get(i);
                    if (seqObj != null) {
                        if (seqObj instanceof Number) {
                            nowMessageSeq = ((Number) seqObj).longValue();
                        } else if (seqObj instanceof String) {
                            try {
                                nowMessageSeq = Long.parseLong((String) seqObj);
                            } catch (NumberFormatException e) {
                                nowMessageSeq = 0L;
                            }
                        }
                    }
                }

                SessionVO sessionVO = SessionVO.builder()
                        .id(group.getId())
                        .type(SessionConstants.SESSION_TYPE_GROUP)
                        .name(group.getGroupName())
                        .avatar(group.getAvatar())
                        .lastMessageTime(lastMsg != null ? lastMsg.getSendTime() : null)
                        .unreadCount(Math.max(0, nowMessageSeq - lastReadSeq))
                        .lastReadSeq(lastReadSeq)
                        .lastMessageSeq(lastMsg != null ? lastMsg.getMessageSeq() : 0L)
                        .build();
                sessionVOList.add(sessionVO);
            }
        }

        //按最后一条消息时间排序
        sessionVOList.sort(Comparator.comparing(SessionVO::getLastMessageTime, Comparator.nullsLast(LocalDateTime::compareTo)).reversed());

        return sessionVOList;
    }

}
