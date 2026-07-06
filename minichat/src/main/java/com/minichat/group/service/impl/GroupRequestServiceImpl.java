package com.minichat.group.service.impl;

import com.minichat.group.constants.GroupConstants;
import com.minichat.common.core.constants.RequestConstants;
import com.minichat.common.core.exception.ErrorCode;
import com.minichat.common.core.exception.GroupException;
import com.minichat.common.security.jwt.UserContext;
import com.minichat.group.dto.GroupRequestDTO;
import com.minichat.group.dto.HandleGroupRequestDTO;
import com.minichat.group.entity.ChatGroup;
import com.minichat.group.entity.GroupMember;
import com.minichat.group.entity.GroupRequest;
import com.minichat.group.mapper.GroupRequestMapper;
import com.minichat.group.service.GroupRequestService;
import com.minichat.group.vo.ReceivedGroupRequestVO;
import com.minichat.group.vo.SentGroupRequestVO;
import com.minichat.user.entity.User;
import com.minichat.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupRequestServiceImpl extends AbstractGroupService implements GroupRequestService {

    private final GroupRequestMapper groupRequestMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public String sendGroupRequest(GroupRequestDTO groupRequestDTO) {
        Long currentUserId = UserContext.getCurUserId();
        User currentUser = userMapper.selectById(currentUserId);
        ChatGroup chatGroup = chatGroupMapper.selectById(groupRequestDTO.getGroupId());
        if (chatGroup == null) {
            throw new GroupException(ErrorCode.GROUP_NOT_FOUND, "群聊不存在");
        }
        Integer joinPolicy = chatGroup.getJoinPolicy();

        if (GroupConstants.JOIN_POLICY_FREEDOM.equals(joinPolicy)) {
            GroupMember groupMember = GroupMember.builder()
                    .groupId(groupRequestDTO.getGroupId())
                    .userId(currentUserId)
                    .role(GroupConstants.ROLE_MEMBER)
                    .isMuted(GroupConstants.NOT_MUTED)
                    .isDeleted(GroupConstants.NOT_DELETED)
                    .nicknameInGroup(currentUser.getNickname())
                    .joinTime(LocalDateTime.now())
                    .build();

            addGroupMemberAndRefreshCache(groupMember);

            return "已加入群聊";
        } else if (GroupConstants.JOIN_POLICY_INVITE.equals(joinPolicy)) {
            throw new GroupException(ErrorCode.GROUP_PERMISSION_DENIED, "加入方式为邀请加入,请让你的好友邀请您加入群聊");
        }

        int isInGroup = groupMemberMapper.selectByGroupIdAndUserId(groupRequestDTO.getGroupId(), currentUserId);
        if (isInGroup > 0) {
            throw new GroupException(ErrorCode.GROUP_ALREADY_IN, "您已经在该群中");
        }

        GroupRequest groupRequest = groupRequestMapper.selectByGroupIdAndUserId(groupRequestDTO.getGroupId(), currentUserId);
        if (groupRequest != null && RequestConstants.PROCESSING.equals(groupRequest.getStatus())) {
            throw new GroupException(ErrorCode.GROUP_ALREADY_IN, "您已经发送过加入该群的请求,请等待处理");
        } else if (groupRequest != null && RequestConstants.REJECTED.equals(groupRequest.getStatus())) {

            groupRequest.setStatus(RequestConstants.PROCESSING);
            groupRequest.setMessage(groupRequestDTO.getMessage());
            groupRequestMapper.update(groupRequest);
            return "您之前加入该群的请求已被拒绝,已重新发送申请";
        }

        GroupRequest newGroupRequest = GroupRequest.builder()
                .groupId(groupRequestDTO.getGroupId())
                .applicantId(currentUserId)
                .message(groupRequestDTO.getMessage())
                .status(RequestConstants.PROCESSING)
                .createdTime(LocalDateTime.now())
                .build();

        groupRequestMapper.insert(newGroupRequest);

        return "加入群聊请求已发送";

    }

    @Override
    public List<SentGroupRequestVO> getSentGroupRequestList(Long currentUserId) {
        List<SentGroupRequestVO> sentGroupRequestVOList = groupRequestMapper.selectSentGroupRequests(currentUserId);
        return sentGroupRequestVOList;
    }

    @Override
    public List<ReceivedGroupRequestVO> getReceivedGroupRequestList(Long currentUserId) {
        return groupRequestMapper.selectReceivedGroupRequests(currentUserId);
    }

    @Override
    @Transactional
    public void handleGroupRequest(HandleGroupRequestDTO handleGroupRequestDTO) {
        Long currentUserId = UserContext.getCurUserId();
        ChatGroup chatGroup = chatGroupMapper.selectById(handleGroupRequestDTO.getGroupId());
        if (chatGroup == null) {
            throw new GroupException(ErrorCode.GROUP_NOT_FOUND, "群聊不存在");
        }

        Integer joinPolicy = chatGroup.getJoinPolicy();
        Integer curUserRole = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(handleGroupRequestDTO.getGroupId(), currentUserId);

        if (GroupConstants.JOIN_POLICY_APPROVAL.equals(joinPolicy)) {
            if (!GroupConstants.ROLE_ADMIN.equals(curUserRole) && !GroupConstants.ROLE_GROUPOWNER.equals(curUserRole)) {
                throw new GroupException(ErrorCode.GROUP_PERMISSION_DENIED, "您没有权限处理该请求");
            }
            GroupRequest groupRequest = groupRequestMapper.selectByGroupIdAndUserId(handleGroupRequestDTO.getGroupId(), handleGroupRequestDTO.getApplicantId());
            if (groupRequest == null) {
                throw new GroupException(ErrorCode.GROUP_NOT_FOUND, "加入群聊请求不存在");
            }
            groupRequest.setStatus(handleGroupRequestDTO.getStatus());
            groupRequest.setProcessedTime(LocalDateTime.now());
            groupRequest.setReviewerId(currentUserId);
            groupRequestMapper.update(groupRequest);

            if (RequestConstants.SUCCESS.equals(handleGroupRequestDTO.getStatus())) {
                int isInGroup = groupMemberMapper.selectByGroupIdAndUserId(handleGroupRequestDTO.getGroupId(), handleGroupRequestDTO.getApplicantId());
                if (isInGroup == 0) {
                    String nicknameInGroup = userMapper.getNickNameById(handleGroupRequestDTO.getApplicantId());
                    GroupMember groupMember = GroupMember.builder()
                            .groupId(handleGroupRequestDTO.getGroupId())
                            .userId(handleGroupRequestDTO.getApplicantId())
                            .role(GroupConstants.ROLE_MEMBER)
                            .isMuted(GroupConstants.NOT_MUTED)
                            .isDeleted(GroupConstants.NOT_DELETED)
                            .nicknameInGroup(nicknameInGroup)
                            .joinTime(LocalDateTime.now())
                            .build();

                    addGroupMemberAndRefreshCache(groupMember);
                }
            }
        }
    }

    private void addGroupMemberAndRefreshCache(GroupMember groupMember) {
        groupMemberMapper.insert(groupMember);

        Long groupId = groupMember.getGroupId();
        updateMemberCountAndRefreshCache(groupId);
    }
}
