package com.minichat.group.service;

import com.minichat.group.vo.GroupMemberVO;
import com.minichat.group.vo.GroupSearchVO;
import com.minichat.group.vo.GroupVO;
import com.minichat.common.result.Result;
import com.minichat.group.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface GroupService {

    /**
     * 创建群聊
     * @param createGroupDTO 创建群聊DTO
     * @return 群聊VO
     */
    GroupVO createGroup(@Valid CreateGroupDTO createGroupDTO);

    /**
     * 获取用户加入的群聊列表
     * @param currentUserId 当前用户ID
     * @return 群聊VO列表
     */
    List<GroupVO> getGroupList(Long currentUserId);

    /**
     * 获取群聊详情
     * @param groupId 群聊ID
     * @return 群聊VO
     */
    GroupVO getGroupProfile(Long groupId);

     /**
     * 更新群聊详情
     * @param groupId 群聊ID
     * @param groupUpdateDTO 更新群聊DTO
     * @return 操作结果
     */
    Result<String> updateGroupProfile(Long groupId, @Valid GroupUpdateDTO groupUpdateDTO);

     /**
     * 上传群聊头像
     * @param groupId 群聊ID
     * @param avatar 群聊头像文件
     * @return 头像URL
     */
    String uploadGroupAvatar(Long groupId, MultipartFile avatar);

    /**
     * 获取群聊成员列表
     * @param groupId 群聊ID
     * @return 群聊成员VO列表
     */
    List<GroupMemberVO> getGroupMemberList(Long groupId);

     /**
     * 邀请用户加入群聊
     * @param groupMemberAddDTO 群聊成员添加DTO
     */
    void inviteUsersToGroup(GroupMemberAddDTO groupMemberAddDTO);

     /**
     * 删除群聊成员
     * @param groupMemberRemoveDTO 群聊成员删除DTO
     */
    void deleteGroupMember(@Valid GroupMemberRemoveDTO groupMemberRemoveDTO);

     /**
     * 退出群聊
     * @param groupId 群聊ID
     * @param curUserId 当前用户ID
     */
    void exitGroup(Long groupId, Long curUserId);

     /**
     * 更新群聊成员角色
     * @param groupMemberRoleUpdateDTO 群聊成员角色更新DTO
     */
    void updateGroupMemberRole(@Valid GroupMemberRoleUpdateDTO groupMemberRoleUpdateDTO);

     /**
     * 转让群聊群主
     * @param groupTransferDTO 群聊转让DTO
     */
    void transferGroupOwner(@Valid GroupTransferDTO groupTransferDTO);

     /**
     * 解散群聊
     * @param groupId 群聊ID
     */
    void dismissGroup(Long groupId);

     /**
     * 搜索群聊
     * @param keyword 搜索关键词
     * @param currentUserId 当前用户ID
     * @return 群聊搜索VO列表
     */
    List<GroupSearchVO> searchGroups(String keyword, Long currentUserId);
}
