package com.minichat.group.service.impl;

import com.minichat.group.dto.GroupRequestDTO;
import com.minichat.group.dto.HandleGroupRequestDTO;
import com.minichat.group.vo.ReceivedGroupRequestVO;
import com.minichat.group.vo.SentGroupRequestVO;
import com.minichat.common.constants.GroupConstants;
import com.minichat.common.constants.RequestConstants;
import com.minichat.group.entity.ChatGroup;
import com.minichat.group.entity.GroupMember;
import com.minichat.group.entity.GroupRequest;
import com.minichat.user.entity.User;
import com.minichat.group.mapper.GroupRequestMapper;
import com.minichat.user.mapper.UserMapper;
import com.minichat.common.result.Result;
import com.minichat.group.service.GroupRequestService;
import com.minichat.common.util.UserContext;
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
    public Result<String> sendGroupRequest(GroupRequestDTO groupRequestDTO) {
        //用户
        Long currentUserId = UserContext.getCurUserId();
        User currentUser = userMapper.selectById(currentUserId);
        //群聊
        ChatGroup chatGroup = chatGroupMapper.selectById(groupRequestDTO.getGroupId());
        if (chatGroup == null) {
            return Result.error("群聊不存在");
        }
        //判断加入方式
        Integer joinPolicy = chatGroup.getJoinPolicy();

        //自由加入，直接加入群聊
        if(GroupConstants.JOIN_POLICY_FREEDOM.equals(joinPolicy)){
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

            return Result.success("已加入群聊");
        }else if(GroupConstants.JOIN_POLICY_INVITE.equals(joinPolicy)){
            //邀请加入，只能让好友邀请
            return Result.error("加入方式为邀请加入,请让你的好友邀请您加入群聊");
        }

        //判断是否在群组中
        int isInGroup = groupMemberMapper.selectByGroupIdAndUserId(groupRequestDTO.getGroupId(), currentUserId);
        if (isInGroup > 0) {
            return Result.error("您已经在该群中");
        }

        GroupRequest groupRequest = groupRequestMapper.selectByGroupIdAndUserId(groupRequestDTO.getGroupId(), currentUserId);
        if (groupRequest != null && RequestConstants.PROCESSING.equals(groupRequest.getStatus())) {
            return Result.error("您已经发送过加入该群的请求,请等待处理");
        }else if(groupRequest != null && RequestConstants.REJECTED.equals(groupRequest.getStatus())){

            groupRequest.setStatus(RequestConstants.PROCESSING);
            groupRequest.setMessage(groupRequestDTO.getMessage());
            groupRequestMapper.update(groupRequest);
            return Result.success("您之前加入该群的请求已被拒绝,以重新发送申请");
        }

        GroupRequest newGroupRequest = GroupRequest.builder()
                .groupId(groupRequestDTO.getGroupId())
                .applicantId(currentUserId)
                .message(groupRequestDTO.getMessage())
                .status(RequestConstants.PROCESSING)
                .createdTime(LocalDateTime.now())
                .build();

        groupRequestMapper.insert(newGroupRequest);

        return Result.success("加入群聊请求已发送");

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
    public Result<String> handleGroupRequest(HandleGroupRequestDTO handleGroupRequestDTO) {
        Long currentUserId = UserContext.getCurUserId();
        ChatGroup chatGroup = chatGroupMapper.selectById(handleGroupRequestDTO.getGroupId());
        if(chatGroup == null){
            return Result.error("群聊不存在");
        }

        //查看加入策略和当前用户角色
        Integer joinPolicy = chatGroup.getJoinPolicy();
        Integer curUserRole = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(handleGroupRequestDTO.getGroupId(), currentUserId);

        if(GroupConstants.JOIN_POLICY_APPROVAL.equals(joinPolicy)){
            //判断权限
            if(!GroupConstants.ROLE_ADMIN.equals(curUserRole) && !GroupConstants.ROLE_GROUPOWNER.equals(curUserRole)){
                return Result.error("您没有权限处理该请求");
            }
            GroupRequest groupRequest = groupRequestMapper.selectByGroupIdAndUserId(handleGroupRequestDTO.getGroupId(), handleGroupRequestDTO.getApplicantId());
            if(groupRequest == null){
                return Result.error("加入群聊请求不存在");
            }
            groupRequest.setStatus(handleGroupRequestDTO.getStatus());
            groupRequest.setProcessedTime(LocalDateTime.now());
            groupRequest.setReviewerId(currentUserId);
            groupRequestMapper.update(groupRequest);

            //处理成功
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

        return Result.success("处理成功");
    }

    /**
     * 添加群成员并刷新相关缓存
     * @param groupMember 待添加的群成员信息
     */
    private void addGroupMemberAndRefreshCache(GroupMember groupMember) {
        // 1. 插入群成员
        groupMemberMapper.insert(groupMember);

        // 2. 更新群成员数量
        Long groupId = groupMember.getGroupId();
        updateMemberCountAndRefreshCache(groupId);
    }
}
