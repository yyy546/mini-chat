package com.minichat.group.service.impl;


import com.alibaba.fastjson2.TypeReference;
import com.minichat.common.constants.RedisConstants;
import com.minichat.group.vo.GroupMemberVO;
import com.minichat.group.vo.GroupSearchVO;
import com.minichat.group.vo.GroupVO;
import com.minichat.common.constants.GroupConstants;
import com.minichat.group.entity.ChatGroup;
import com.minichat.group.entity.GroupMember;
import com.minichat.group.dto.*;
import com.minichat.chat.mapper.GroupMessageMapper;
import com.minichat.user.mapper.UserMapper;
import com.minichat.common.result.Result;
import com.minichat.group.service.GroupService;
import com.minichat.common.util.OssFileUtil;
import com.minichat.common.util.UserContext;
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

        // 处理成员ID列表
        List<Long> memberIds = createGroupDTO.getMemberIds();
        List<String> keys = new ArrayList<>();
        if(memberIds == null){
            memberIds = new ArrayList<>();
        }
        // 确保创建者在成员列表中（如果尚未包含）
        if(!memberIds.contains(userId)) {
            memberIds.add(0, userId);
        }
        int memberCount = memberIds.size();

        // 处理最大成员数量
        Integer maxMembers = createGroupDTO.getMaxMembers();
        if(maxMembers == null){
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

        // 保存群组
        chatGroupMapper.insert(group);

        // 保存群组成员
        for (Long memberId : memberIds) {
            // 群主角色为2，其他成员角色为0
            int role = GroupConstants.ROLE_MEMBER;
            if(memberId.equals(userId)){
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
            // 缓存用户的群组列表
            keys.add(RedisConstants.CACHE_USER_GROUP_LIST_KEY_PREFIX + memberId);
        }
        cacheClient.deleteBatch(keys);

        //初始管理员列表为空
        List<Long> adminIds = new ArrayList<>();

        // 构建返回的VO
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
                    //先查基本属性
                    List<GroupVO> list = groupMemberMapper.selectGroupVOByUserId(id);
                    //填充管理员ID列表
                    for(GroupVO groupVO : list){
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

                    // 设置管理员ID列表
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
        // 检查群组是否存在
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if(group == null){
            throw new IllegalArgumentException("群组不存在或已被删除");
        }

        // 检查当前用户是否是群组管理员或群主
        Integer userRole = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupId, currentUserId);
        boolean isAdmin = Objects.equals(userRole, GroupConstants.ROLE_ADMIN);
        boolean isOwner = group.getOwnerId().equals(currentUserId);
        if(!isAdmin && !isOwner){
            throw new IllegalArgumentException("只有群组所有者或管理员才能上传群组头像");
        }

        if(avatar == null || avatar.isEmpty()){
            throw new IllegalArgumentException("群组头像不能为空");
        }

        String newAvatarUrl = null;
        try {
            // ========== 关键修改：增加默认头像判断，仅删除非默认的旧头像 ==========
            String oldAvatarUrl = group.getAvatar();
            if (StringUtils.isNotBlank(oldAvatarUrl)) {
                // 用Objects.equals避免空指针，且匹配更严谨
                if (!Objects.equals(oldAvatarUrl, GroupConstants.DEFAULT_GROUP_AVATAR)) {
                    ossFileUtil.deleteFile(oldAvatarUrl);
                    log.info("群组{}旧头像（非默认）已从OSS删除，URL：{}", groupId, oldAvatarUrl);
                } else {
                    log.info("群组{}旧头像为系统默认头像，跳过删除", groupId);
                }
            }
            // 2.2 上传新头像到OSS，获取新URL
            newAvatarUrl = ossFileUtil.uploadFile(avatar, ossGroupAvatarPath);
            log.info("群组{}新头像上传OSS成功，URL：{}", groupId, newAvatarUrl);
        } catch (IOException e) {
            log.error("群组{}头像文件流异常", groupId, e);
            throw new RuntimeException("头像上传失败：文件流异常");
        } catch (IllegalArgumentException e) {
            log.error("群组{}头像参数非法", groupId, e);
            throw new RuntimeException("头像上传失败：" + e.getMessage());
        } catch (RuntimeException e) {
            log.error("群组{}OSS上传失败", groupId, e);
            throw new RuntimeException("头像上传失败：" + e.getMessage());
        }

        // 上传群组头像
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

        // 1. 检查群组是否存在
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("群组不存在");
        }

        // 2. 过滤已存在的成员
        List<Long> existingMemberIds = groupMemberMapper.selectMemberIdsByGroupId(groupId);
        List<Long> newMemberIds = new ArrayList<>();
        for (Long inviteeId : inviteIds) {
            if (!existingMemberIds.contains(inviteeId)) {
                newMemberIds.add(inviteeId);
            }
        }

        if (newMemberIds.isEmpty()) {
            throw new IllegalArgumentException("所选用户均已在群中");
        }

        // 3. 检查邀请权限
        Integer invitePolicy = group.getInvitePolicy();
        Integer role = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupId, currentUserId);
        
        if (role == null) {
            throw new IllegalArgumentException("您不是该群成员，无法邀请");
        }
        
        if(role < invitePolicy){
            throw new IllegalArgumentException("您的权限不足，无法邀请该群成员");
        }
        
        // 检查群成员数量限制
        if (existingMemberIds.size() + newMemberIds.size() > group.getMaxMembers()) {
            throw new IllegalArgumentException("群成员数量已达上限，无法继续邀请");
        }

        // 4. 批量添加成员
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
        
        // 5. 更新群成员数量
        chatGroupMapper.updateMemberCount(groupId, existingMemberIds.size() + newMemberIds.size());
        // 6. 删除所有用户的群组缓存
        refreshGroupMemberCache(groupId);

        // 7. 删除群组资料缓存
        cacheClient.delete(RedisConstants.CACHE_GROUP_PROFILE_KEY_PREFIX + groupId);
    }

    @Override
    public void deleteGroupMember(GroupMemberRemoveDTO groupMemberRemoveDTO) {
        Long currentUserId = UserContext.getCurUserId();
        Integer role = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupMemberRemoveDTO.getGroupId(), currentUserId);

        if(role == null){
            throw new IllegalArgumentException("您不是该群成员，无法删除");
        }

        if(role < GroupConstants.ROLE_ADMIN){
            throw new IllegalArgumentException("您的权限不足，无法删除该群成员");
        }

        //查看删除的是否为管理员或群主
        Integer targetRole = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupMemberRemoveDTO.getGroupId(), groupMemberRemoveDTO.getUserId());

        if(targetRole == null){
            throw new IllegalArgumentException("该群成员不存在");
        }

        if(targetRole >= role){
            throw new IllegalArgumentException("您的权限不足，无法删除该群成员");
        }

        //删除群组成员
        groupMemberMapper.deleteByGroupIdAndUserId(groupMemberRemoveDTO.getGroupId(), groupMemberRemoveDTO.getUserId());
        // 更新群成员数量
        int memberCount = groupMemberMapper.selectMemberIdsByGroupId(groupMemberRemoveDTO.getGroupId()).size();
        chatGroupMapper.updateMemberCount(groupMemberRemoveDTO.getGroupId(), memberCount);
        // 删除所有用户的群组缓存
        refreshGroupMemberCache(groupMemberRemoveDTO.getGroupId());
        // 删除被移除成员的缓存
        String exitUserCacheKey = RedisConstants.CACHE_USER_GROUP_LIST_KEY_PREFIX + groupMemberRemoveDTO.getUserId();
        cacheClient.delete(exitUserCacheKey);
        // 删除群组资料缓存
        cacheClient.delete(RedisConstants.CACHE_GROUP_PROFILE_KEY_PREFIX + groupMemberRemoveDTO.getGroupId());
    }

    @Override
    @Transactional
    public void exitGroup(Long groupId, Long curUserId) {
        // 检查用户是否是群成员
        Integer userRole = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupId, curUserId);
        if(userRole == null){
            throw new IllegalArgumentException("您不是该群成员，无法退出");
        }

        //查看是否为群主
        if(userRole.equals(GroupConstants.ROLE_GROUPOWNER)){
            int memberCount = groupMemberMapper.selectMemberIdsByGroupId(groupId).size();
            if(memberCount == 1){
                //删除群聊消息
                groupMessageMapper.deleteByGroupId(groupId);
                //删除群聊成员
                groupMemberMapper.deleteByGroupIdAndUserId(groupId, curUserId);
                //删除群聊
                chatGroupMapper.deleteById(groupId);
                return;
            }else{
                throw new IllegalArgumentException("群主不能退出群聊，请先转让群主");
            }
        }

        // 删除群组成员
        groupMemberMapper.deleteByGroupIdAndUserId(groupId, curUserId);
        // 删除所有用户的群组缓存
        updateMemberCountAndRefreshCache(groupId);
        // 删除退出成员的缓存
        String exitUserCacheKey = RedisConstants.CACHE_USER_GROUP_LIST_KEY_PREFIX + curUserId;
        cacheClient.delete(exitUserCacheKey);
        // 删除群组资料缓存
        cacheClient.delete(RedisConstants.CACHE_GROUP_PROFILE_KEY_PREFIX + groupId);
    }

    @Override
    public void updateGroupMemberRole(GroupMemberRoleUpdateDTO groupMemberRoleUpdateDTO) {
        Long currentUserId = UserContext.getCurUserId();
        Long groupId = groupMemberRoleUpdateDTO.getGroupId();
        Integer newRole = groupMemberRoleUpdateDTO.getRole();

        // 1. 只有群主可以设置或取消管理员
        Integer currentUserRole = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupId, currentUserId);
        if (currentUserRole == null || !currentUserRole.equals(GroupConstants.ROLE_GROUPOWNER)) {
            throw new IllegalArgumentException("只有群主可以设置或取消管理员");
        }

        // 2. 检查目标用户是否是群成员
        Long targetUserId = groupMemberRoleUpdateDTO.getUserId();
        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("群主不能修改自己的角色，请使用转让群主功能");
        }

        Integer targetUserRole = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupId, targetUserId);
        if (targetUserRole == null) {
            throw new IllegalArgumentException("该用户不是群成员");
        }

        if (targetUserRole.equals(GroupConstants.ROLE_GROUPOWNER)) {
            throw new IllegalArgumentException("不能修改群主的角色");
        }

        // 3. 校验目标角色（只能设置为管理员或普通成员）
        if (!newRole.equals(GroupConstants.ROLE_ADMIN) && !newRole.equals(GroupConstants.ROLE_MEMBER)) {
            throw new IllegalArgumentException("只能设置管理员或普通成员身份");
        }

        // 4. 更新角色
        groupMemberMapper.updateRole(groupId, targetUserId, newRole);
        // 删除所有用户的群组缓存
        refreshGroupMemberCache(groupId);
        // 删除群组资料缓存
        cacheClient.delete(RedisConstants.CACHE_GROUP_PROFILE_KEY_PREFIX + groupId);
    }

    @Override
    @Transactional
    public void transferGroupOwner(GroupTransferDTO groupTransferDTO) {
        Long currentUserId = UserContext.getCurUserId();
        Long newOwnerId = groupTransferDTO.getNewOwnerId();
        Long groupId = groupTransferDTO.getGroupId();

        if (currentUserId.equals(newOwnerId)) {
            throw new IllegalArgumentException("不能转让给自己");
        }

        // 1. 检查当前用户是否是群主
        Integer currentUserRole = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupId, currentUserId);
        if (currentUserRole == null || !currentUserRole.equals(GroupConstants.ROLE_GROUPOWNER)) {
            throw new IllegalArgumentException("只有群主可以转让群组");
        }

        // 2. 检查新群主是否是群成员
        Integer newOwnerRole = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupId, newOwnerId);
        if (newOwnerRole == null) {
            throw new IllegalArgumentException("新群主必须是群成员");
        }

        // 3. 将旧群主降级为普通成员
        groupMemberMapper.updateRole(groupId, currentUserId, GroupConstants.ROLE_MEMBER);
        // 4. 将新群主升级为群主
        groupMemberMapper.updateRole(groupId, newOwnerId, GroupConstants.ROLE_GROUPOWNER);
        // 5. 更新群组信息中的群主ID
        chatGroupMapper.updateOwner(groupId, newOwnerId);
        // 删除所有用户的群组缓存
        refreshGroupMemberCache(groupId);
        // 删除群组资料缓存
        cacheClient.delete(RedisConstants.CACHE_GROUP_PROFILE_KEY_PREFIX + groupId);
    }

    @Override
    public void dismissGroup(Long groupId) {
        // 1. 检查群组是否存在
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("群组不存在或已被删除");
        }

        //2. 查看是否为群主
        Long currentUserId = UserContext.getCurUserId();
        Integer currentUserRole = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupId, currentUserId);
        if (currentUserRole == null || !currentUserRole.equals(GroupConstants.ROLE_GROUPOWNER)) {
            throw new IllegalArgumentException("只有群主可以解散群组");
        }

        // 3. 删除群组成员
        groupMemberMapper.deleteByGroupId(groupId);

        // 4. 删除群聊消息
        groupMessageMapper.deleteByGroupId(groupId);

        // 5. 删除群组
        chatGroupMapper.deleteById(groupId);

        // 删除所有用户的群组缓存
        refreshGroupMemberCache(groupId);
        // 删除群组资料缓存
        cacheClient.delete(RedisConstants.CACHE_GROUP_PROFILE_KEY_PREFIX + groupId);
    }

    @Override
    public List<GroupSearchVO> searchGroups(String keyword, Long currentUserId) {
        //搜索群组
        List<GroupSearchVO> groupSearchVOList = chatGroupMapper.searchGroups(keyword, currentUserId);
        return groupSearchVOList;
    }

    @Override
    public Result<String> updateGroupProfile(Long groupId, GroupUpdateDTO groupUpdateDTO) {
        ChatGroup group = chatGroupMapper.selectById(groupId);

        if(group == null){
            return Result.error("群组不存在或已被删除");
        }

        Long currentUserId = UserContext.getCurUserId();

        // 检查当前用户是否是群组管理员或群主
        Integer userRole = groupMemberMapper.selectGroupMemberRoleByGroupIdAndUserId(groupId, currentUserId);
        boolean isAdmin = Objects.equals(userRole, GroupConstants.ROLE_ADMIN);
        boolean isOwner = group.getOwnerId().equals(currentUserId);
        if(!isAdmin && !isOwner){
            return Result.error("只有群组所有者或管理员才能更新群组信息");
        }

        // 更新群组信息
        group.setGroupName(groupUpdateDTO.getGroupName());
        // 检查是否更新了群头像
        if(groupUpdateDTO.getAvatar() != null  && !groupUpdateDTO.getAvatar().trim().isEmpty()){
            group.setAvatar(groupUpdateDTO.getAvatar());
            log.info("用户{}更新群组{}头像为: {}", currentUserId, groupId, groupUpdateDTO.getAvatar());
        }

        group.setAnnouncement(groupUpdateDTO.getAnnouncement());
        group.setMaxMembers(groupUpdateDTO.getMaxMembers());
        group.setJoinPolicy(groupUpdateDTO.getJoinPolicy());
        group.setInvitePolicy(groupUpdateDTO.getInvitePolicy());
        group.setUpdatedTime(LocalDateTime.now());
        // 更新群组信息
        chatGroupMapper.update(group);
        // 删除所有用户的群组缓存
        refreshGroupMemberCache(groupId);
        // 删除群组资料缓存
        cacheClient.delete(RedisConstants.CACHE_GROUP_PROFILE_KEY_PREFIX + groupId);

        return Result.success("更新群组信息成功");
    }

}
