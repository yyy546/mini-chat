package com.minichat.group.service;

import com.minichat.group.vo.GroupMemberVO;
import com.minichat.group.vo.GroupSearchVO;
import com.minichat.group.vo.GroupVO;
import com.minichat.group.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface GroupService {

    GroupVO createGroup(@Valid CreateGroupDTO createGroupDTO);

    List<GroupVO> getGroupList(Long currentUserId);

    GroupVO getGroupProfile(Long groupId);

    void updateGroupProfile(Long groupId, @Valid GroupUpdateDTO groupUpdateDTO);

    String uploadGroupAvatar(Long groupId, MultipartFile avatar);

    List<GroupMemberVO> getGroupMemberList(Long groupId);

    void inviteUsersToGroup(GroupMemberAddDTO groupMemberAddDTO);

    void deleteGroupMember(@Valid GroupMemberRemoveDTO groupMemberRemoveDTO);

    void exitGroup(Long groupId, Long curUserId);

    void updateGroupMemberRole(@Valid GroupMemberRoleUpdateDTO groupMemberRoleUpdateDTO);

    void transferGroupOwner(@Valid GroupTransferDTO groupTransferDTO);

    void dismissGroup(Long groupId);

    List<GroupSearchVO> searchGroups(String keyword, Long currentUserId);
}
