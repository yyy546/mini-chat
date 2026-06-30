package com.minichat.group.service.impl;

import com.alibaba.fastjson2.TypeReference;
import com.minichat.common.constants.GroupConstants;
import com.minichat.common.constants.RedisConstants;
import com.minichat.common.exception.ErrorCode;
import com.minichat.common.exception.GroupException;
import com.minichat.common.util.OssFileUtil;
import com.minichat.common.util.UserContext;
import com.minichat.group.dto.CreateGroupDTO;
import com.minichat.group.dto.GroupMemberAddDTO;
import com.minichat.group.dto.GroupMemberRemoveDTO;
import com.minichat.group.dto.GroupMemberRoleUpdateDTO;
import com.minichat.group.dto.GroupTransferDTO;
import com.minichat.group.dto.GroupUpdateDTO;
import com.minichat.group.entity.ChatGroup;
import com.minichat.group.entity.GroupMember;
import com.minichat.group.service.GroupService;
import com.minichat.group.vo.GroupMemberVO;
import com.minichat.group.vo.GroupSearchVO;
import com.minichat.group.vo.GroupVO;
import com.minichat.chat.mapper.GroupMessageMapper;
import com.minichat.user.mapper.UserMapper;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl extends AbstractGroupService implements GroupService {

    private final UserMapper userMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final OssFileUtil ossFileUtil;

    @Value("${aliyun.oss.group-avatar-path:group-avatar/}")
    private String ossGroupAvatarPath;

    @Override
    @Transactional
    public GroupVO createGroup(CreateGroupDTO createGroupDTO) {
        Long userId = UserContext.getCurUserId();

        List<Long> memberIds = createGroupDTO.getMemberIds();
        List<String> keys = new ArrayList<>();
        if (memberIds == null) {
            memberIds = new ArrayList<>();
        }
        if (!memberIds.contains(userId)) {
            memberIds.add(0, userId);
        }
        int memberCount = memberIds.size();

        Integer maxMembers = createGroupDTO.getMaxMembers();
        if (maxMembers == null) {
            maxMembers = GroupConstants.DEFAULT_MAX_MEMBERS;
        }

        ChatGroup group = ChatGroup.builder()
                .groupName(createGroupDTO.getGroupName())
                .avatar(GroupConstants.DEFAULT_GROUP_AVATAR)
                .announcement(createGroupDTO.getAnnouncement())
                .creatorId(userId)
                .ownerId(userId)
                .memberCount(memberCount)
                .maxMembers(maxMembers)
                .joinPolicy(createGroupDTO.getJoinPolicy())
                .invitePolicy(createGroupDTO.getInvitePolicy())
                .isDeleted(GroupConstants.NOT_DELETED)
                .createdTime(LocalDateTime.now())
                .updatedTime(LocalDateTime.now())
                .build();

        chatGroupMapper.insert(group);

        for (Long memberId : memberIds) {
            int role = GroupConstants.ROLE_MEMBER;
            if (memberId.equals(userId)) {
                role = GroupConstants.ROLE_GROUPOWNER;
            }

            GroupMember groupMember = GroupMember.builder()
                    .groupId(group.getId())
                    .userId(memberId)
                    .role(role)
                    .isMuted(GroupConstants.NOT_MUTED)
                    .isDeleted(GroupConstants.NOT_DELETED)
                    .nicknameInGroup(userMapper.getNickNameById(memberId))
                    .joinTime(LocalDateTime.now())
                    .build();
            groupMemberMapper.insert(groupMember);
            keys.add(RedisConstants.CACHE_USER_GROUP_LIST_KEY_PREFIX + memberId);
        }
        cacheClient.deleteBatch(keys);

        List<Long> adminIds = new ArrayList<>();

        GroupVO groupVO = GroupVO.builder()
                .id(group.getId())
                .groupName(group.getGroupName())
                .avatar(group.getAvatar())
                .announcement(group.getAnnouncement())
                .creatorId(userId)
                .ownerId(userId)
                .memberCount(group.getMemberCount())
                .maxMembers(group.getMaxMembers())
                .joinPolicy(group.getJoinPolicy())
                .invitePolicy(group.getInvitePolicy())
                .createdTime(group.getCreatedTime())
                .adminIds(adminIds)
                .build();

        return groupVO;
    }

    @Override
    public List<GroupVO> getGroupList(Long currentUserId) {
        List<GroupVO> groupVOList = cacheClient.queryWithPassThrough(
                RedisConstants.CACHE_USER_GROUP_LIST_KEY_PREFIX, currentUserId,
                new TypeReference<List<GroupVO>>() {},
                id -> {
                    List<GroupVO> list = groupMemberMapper.selectGroupVOByUserId(id);
                    for (GroupVO groupVO : list) {
                        List<Long> adminIds = groupMemberMapper.selectAdminIdsByGroupId(groupVO.getId());
                        groupVO.setAdminIds(adminIds);
                    }
                    return list;
                },
                RedisConstants.CACHE_NORMAL_EXPIRE_TIME + new Random().nextLong(10), TimeUnit.MINUTES);

        return groupVOList;
    }

    @Override
    public GroupVO getGroupProfile(Long groupId) {
        GroupVO groupVO = cacheClient.queryWithPassThrough(
                RedisConstants.CACHE_GROUP_PROFILE_KEY_PREFIX, groupId,
                new TypeReference<GroupVO>() {},
                id -> {
                    GroupVO groupVOById = chatGroupMapper.selectGroupVOById(groupId);

                    List<Long> adminIds = groupMemberMapper.selectAdminIdsByGroupId(groupId);
                    groupVOById.setAdminIds(adminIds);

                    return groupVOById;
                },
                RedisConstants.CACHE_NORMAL_EXPIRE_TIME + new Random().nextLong(10), TimeUnit.MINUTES);

        return groupVO;
    }

    @Override
    public String uploadGroupAvatar(Long groupId, MultipartFile avatar) {
        Long currentUserId = UserContext.getCurUserId();
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null) {
            throw new GroupException(ErrorCode.GROUP_NOT_FOUND, "群组不存在或已被删除");
        }

        Integer userRole = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupId, currentUserId);
        boolean isAdmin = Objects.equals(userRole, GroupConstants.ROLE_ADMIN);
        boolean isOwner = group.getOwnerId().equals(currentUserId);
        if (!isAdmin && !isOwner) {
            throw new GroupException(ErrorCode.GROUP_PERMISSION_DENIED, "只有群组所有者或管理员才能上传群组头像");
        }

        if (avatar == null || avatar.isEmpty()) {
            throw new GroupException(ErrorCode.BAD_REQUEST, "群组头像不能为空");
        }

        String newAvatarUrl = null;
        try {
            String oldAvatarUrl = group.getAvatar();
            if (StringUtils.isNotBlank(oldAvatarUrl)) {
                if (!Objects.equals(oldAvatarUrl, GroupConstants.DEFAULT_GROUP_AVATAR)) {
                    ossFileUtil.deleteFile(oldAvatarUrl);
                    log.info("群组{}旧头像（非默认）已从OSS删除，URL：{}", groupId, oldAvatarUrl);
                } else {
                    log.info("群组{}旧头像为系统默认头像，跳过删除", groupId);
                }
            }
            newAvatarUrl = ossFileUtil.uploadFile(avatar, ossGroupAvatarPath);
            log.info("群组{}新头像上传OSS成功，URL：{}", groupId, newAvatarUrl);
        } catch (IOException e) {
            log.error("群组{}头像文件流异常", groupId, e);
            throw new GroupException(ErrorCode.INTERNAL_ERROR, "头像上传失败：文件流异常");
        }

        chatGroupMapper.updateAvatar(groupId, newAvatarUrl);

        return newAvatarUrl;
    }

    @Override
    public List<GroupMemberVO> getGroupMemberList(Long groupId) {
        return groupMemberMapper.selectGroupMemberVOByGroupId(groupId);
    }

    @Override
    @Transactional
    public void inviteUsersToGroup(GroupMemberAddDTO groupMemberAddDTO) {
        Long groupId = groupMemberAddDTO.getGroupId();
        List<Long> inviteIds = groupMemberAddDTO.getUserIds();
        Long currentUserId = UserContext.getCurUserId();

        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null) {
            throw new GroupException(ErrorCode.GROUP_NOT_FOUND, "群组不存在");
        }

        List<Long> existingMemberIds = groupMemberMapper.selectMemberIdsByGroupId(groupId);
        List<Long> newMemberIds = new ArrayList<>();
        for (Long inviteeId : inviteIds) {
            if (!existingMemberIds.contains(inviteeId)) {
                newMemberIds.add(inviteeId);
            }
        }

        if (newMemberIds.isEmpty()) {
            throw new GroupException(ErrorCode.GROUP_ALREADY_IN, "所选用户均已在群中");
        }

        Integer invitePolicy = group.getInvitePolicy();
        Integer role = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupId, currentUserId);

        if (role == null) {
            throw new GroupException(ErrorCode.NOT_GROUP_MEMBER, "您不是该群成员，无法邀请");
        }

        if (role < invitePolicy) {
            throw new GroupException(ErrorCode.GROUP_PERMISSION_DENIED, "您的权限不足，无法邀请该群成员");
        }

        if (existingMemberIds.size() + newMemberIds.size() > group.getMaxMembers()) {
            throw new GroupException(ErrorCode.GROUP_FULL, "群成员数量已达上限，无法继续邀请");
        }

        for (Long newMemberId : newMemberIds) {
            String nicknameInGroup = userMapper.getNickNameById(newMemberId);
            GroupMember groupMember = GroupMember.builder()
                    .groupId(groupId)
                    .userId(newMemberId)
                    .role(GroupConstants.ROLE_MEMBER)
                    .isMuted(GroupConstants.NOT_MUTED)
                    .isDeleted(GroupConstants.NOT_DELETED)
                    .nicknameInGroup(nicknameInGroup)
                    .joinTime(LocalDateTime.now())
                    .build();
            groupMemberMapper.insert(groupMember);
        }

        chatGroupMapper.updateMemberCount(groupId, existingMemberIds.size() + newMemberIds.size());
        refreshGroupMemberCache(groupId);

        cacheClient.delete(RedisConstants.CACHE_GROUP_PROFILE_KEY_PREFIX + groupId);
    }

    @Override
    public void deleteGroupMember(GroupMemberRemoveDTO groupMemberRemoveDTO) {
        Long currentUserId = UserContext.getCurUserId();
        Integer role = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupMemberRemoveDTO.getGroupId(), currentUserId);

        if (role == null) {
            throw new GroupException(ErrorCode.NOT_GROUP_MEMBER, "您不是该群成员，无法删除");
        }

        if (role < GroupConstants.ROLE_ADMIN) {
            throw new GroupException(ErrorCode.GROUP_PERMISSION_DENIED, "您的权限不足，无法删除该群成员");
        }

        Integer targetRole = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupMemberRemoveDTO.getGroupId(), groupMemberRemoveDTO.getUserId());

        if (targetRole == null) {
            throw new GroupException(ErrorCode.NOT_GROUP_MEMBER, "该群成员不存在");
        }

        if (targetRole >= role) {
            throw new GroupException(ErrorCode.GROUP_PERMISSION_DENIED, "您的权限不足，无法删除该群成员");
        }

        groupMemberMapper.deleteByGroupIdAndUserId(groupMemberRemoveDTO.getGroupId(), groupMemberRemoveDTO.getUserId());
        int memberCount = groupMemberMapper.selectMemberIdsByGroupId(groupMemberRemoveDTO.getGroupId()).size();
        chatGroupMapper.updateMemberCount(groupMemberRemoveDTO.getGroupId(), memberCount);
        refreshGroupMemberCache(groupMemberRemoveDTO.getGroupId());
        String exitUserCacheKey = RedisConstants.CACHE_USER_GROUP_LIST_KEY_PREFIX + groupMemberRemoveDTO.getUserId();
        cacheClient.delete(exitUserCacheKey);
        cacheClient.delete(RedisConstants.CACHE_GROUP_PROFILE_KEY_PREFIX + groupMemberRemoveDTO.getGroupId());
    }

    @Override
    @Transactional
    public void exitGroup(Long groupId, Long curUserId) {
        Integer userRole = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupId, curUserId);
        if (userRole == null) {
            throw new GroupException(ErrorCode.NOT_GROUP_MEMBER, "您不是该群成员，无法退出");
        }

        if (userRole.equals(GroupConstants.ROLE_GROUPOWNER)) {
            int memberCount = groupMemberMapper.selectMemberIdsByGroupId(groupId).size();
            if (memberCount == 1) {
                groupMessageMapper.deleteByGroupId(groupId);
                groupMemberMapper.deleteByGroupIdAndUserId(groupId, curUserId);
                chatGroupMapper.deleteById(groupId);
                return;
            } else {
                throw new GroupException(ErrorCode.GROUP_OWNER_CANNOT_EXIT, "群主不能退出群聊，请先转让群主");
            }
        }

        groupMemberMapper.deleteByGroupIdAndUserId(groupId, curUserId);
        updateMemberCountAndRefreshCache(groupId);
        String exitUserCacheKey = RedisConstants.CACHE_USER_GROUP_LIST_KEY_PREFIX + curUserId;
        cacheClient.delete(exitUserCacheKey);
        cacheClient.delete(RedisConstants.CACHE_GROUP_PROFILE_KEY_PREFIX + groupId);
    }

    @Override
    public void updateGroupMemberRole(GroupMemberRoleUpdateDTO groupMemberRoleUpdateDTO) {
        Long currentUserId = UserContext.getCurUserId();
        Long groupId = groupMemberRoleUpdateDTO.getGroupId();
        Integer newRole = groupMemberRoleUpdateDTO.getRole();

        Integer currentUserRole = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupId, currentUserId);
        if (currentUserRole == null || !currentUserRole.equals(GroupConstants.ROLE_GROUPOWNER)) {
            throw new GroupException(ErrorCode.GROUP_PERMISSION_DENIED, "只有群主可以设置或取消管理员");
        }

        Long targetUserId = groupMemberRoleUpdateDTO.getUserId();
        if (currentUserId.equals(targetUserId)) {
            throw new GroupException(ErrorCode.BAD_REQUEST, "群主不能修改自己的角色，请使用转让群主功能");
        }

        Integer targetUserRole = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupId, targetUserId);
        if (targetUserRole == null) {
            throw new GroupException(ErrorCode.NOT_GROUP_MEMBER, "该用户不是群成员");
        }

        if (targetUserRole.equals(GroupConstants.ROLE_GROUPOWNER)) {
            throw new GroupException(ErrorCode.GROUP_PERMISSION_DENIED, "不能修改群主的角色");
        }

        if (!newRole.equals(GroupConstants.ROLE_ADMIN) && !newRole.equals(GroupConstants.ROLE_MEMBER)) {
            throw new GroupException(ErrorCode.BAD_REQUEST, "只能设置管理员或普通成员身份");
        }

        groupMemberMapper.updateRole(groupId, targetUserId, newRole);
        refreshGroupMemberCache(groupId);
        cacheClient.delete(RedisConstants.CACHE_GROUP_PROFILE_KEY_PREFIX + groupId);
    }

    @Override
    @Transactional
    public void transferGroupOwner(GroupTransferDTO groupTransferDTO) {
        Long currentUserId = UserContext.getCurUserId();
        Long newOwnerId = groupTransferDTO.getNewOwnerId();
        Long groupId = groupTransferDTO.getGroupId();

        if (currentUserId.equals(newOwnerId)) {
            throw new GroupException(ErrorCode.BAD_REQUEST, "不能转让给自己");
        }

        Integer currentUserRole = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupId, currentUserId);
        if (currentUserRole == null || !currentUserRole.equals(GroupConstants.ROLE_GROUPOWNER)) {
            throw new GroupException(ErrorCode.GROUP_PERMISSION_DENIED, "只有群主可以转让群组");
        }

        Integer newOwnerRole = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupId, newOwnerId);
        if (newOwnerRole == null) {
            throw new GroupException(ErrorCode.NOT_GROUP_MEMBER, "新群主必须是群成员");
        }

        groupMemberMapper.updateRole(groupId, currentUserId, GroupConstants.ROLE_MEMBER);
        groupMemberMapper.updateRole(groupId, newOwnerId, GroupConstants.ROLE_GROUPOWNER);
        chatGroupMapper.updateOwner(groupId, newOwnerId);
        refreshGroupMemberCache(groupId);
        cacheClient.delete(RedisConstants.CACHE_GROUP_PROFILE_KEY_PREFIX + groupId);
    }

    @Override
    public void dismissGroup(Long groupId) {
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null) {
            throw new GroupException(ErrorCode.GROUP_NOT_FOUND, "群组不存在或已被删除");
        }

        Long currentUserId = UserContext.getCurUserId();
        Integer currentUserRole = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupId, currentUserId);
        if (currentUserRole == null || !currentUserRole.equals(GroupConstants.ROLE_GROUPOWNER)) {
            throw new GroupException(ErrorCode.GROUP_PERMISSION_DENIED, "只有群主可以解散群组");
        }

        groupMemberMapper.deleteByGroupId(groupId);

        groupMessageMapper.deleteByGroupId(groupId);

        chatGroupMapper.deleteById(groupId);

        refreshGroupMemberCache(groupId);
        cacheClient.delete(RedisConstants.CACHE_GROUP_PROFILE_KEY_PREFIX + groupId);
    }

    @Override
    public List<GroupSearchVO> searchGroups(String keyword, Long currentUserId) {
        List<GroupSearchVO> groupSearchVOList = chatGroupMapper.searchGroups(keyword, currentUserId);
        return groupSearchVOList;
    }

    @Override
    public void updateGroupProfile(Long groupId, GroupUpdateDTO groupUpdateDTO) {
        ChatGroup group = chatGroupMapper.selectById(groupId);

        if (group == null) {
            throw new GroupException(ErrorCode.GROUP_NOT_FOUND, "群组不存在或已被删除");
        }

        Long currentUserId = UserContext.getCurUserId();

        Integer userRole = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupId, currentUserId);
        boolean isAdmin = Objects.equals(userRole, GroupConstants.ROLE_ADMIN);
        boolean isOwner = group.getOwnerId().equals(currentUserId);
        if (!isAdmin && !isOwner) {
            throw new GroupException(ErrorCode.GROUP_PERMISSION_DENIED, "只有群组所有者或管理员才能更新群组信息");
        }

        group.setGroupName(groupUpdateDTO.getGroupName());
        if (groupUpdateDTO.getAvatar() != null && !groupUpdateDTO.getAvatar().trim().isEmpty()) {
            group.setAvatar(groupUpdateDTO.getAvatar());
            log.info("用户{}更新群组{}头像为: {}", currentUserId, groupId, groupUpdateDTO.getAvatar());
        }

        group.setAnnouncement(groupUpdateDTO.getAnnouncement());
        group.setMaxMembers(groupUpdateDTO.getMaxMembers());
        group.setJoinPolicy(groupUpdateDTO.getJoinPolicy());
        group.setInvitePolicy(groupUpdateDTO.getInvitePolicy());
        group.setUpdatedTime(LocalDateTime.now());
        chatGroupMapper.update(group);
        refreshGroupMemberCache(groupId);
        cacheClient.delete(RedisConstants.CACHE_GROUP_PROFILE_KEY_PREFIX + groupId);
    }

}
