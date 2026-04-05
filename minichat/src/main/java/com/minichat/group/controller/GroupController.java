package com.minichat.group.controller;

import com.minichat.group.vo.GroupMemberVO;
import com.minichat.group.vo.GroupSearchVO;
import com.minichat.group.vo.GroupVO;
import com.minichat.common.result.Result;
import com.minichat.group.dto.*;
import com.minichat.group.service.GroupService;
import com.minichat.common.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    // 创建群组
    @PostMapping("/create")
    public Result<GroupVO> createGroup(@Valid @RequestBody CreateGroupDTO createGroupDTO) {
        GroupVO groupVO = groupService.createGroup(createGroupDTO);
        return Result.success("创建群组成功", groupVO);
    }

    // 获取用户加入的群组列表
    @GetMapping("/list")
    public Result<List<GroupVO>> getGroupList() {
        Long currentUserId = UserContext.getCurUserId();
        if (currentUserId == null) {
            return Result.error("用户未登录");
        }
        List<GroupVO> groupVOList = groupService.getGroupList(currentUserId);
        return Result.success("获取群组列表成功", groupVOList);
    }

    @GetMapping("/profile/{groupId}")
    public Result<GroupVO> getGroupProfile(@PathVariable Long groupId) {
        if (groupId == null) {
            return Result.error("群组ID不能为空");
        }

        GroupVO groupVO = groupService.getGroupProfile(groupId);

        if (groupVO == null) {
            return Result.error("群组不存在");
        }

        return Result.success("获取群组详情成功", groupVO);
    }

    // 更新群组头像
    @PostMapping("/avatar/{groupId}")
    public Result<String> uploadGroupAvatar(@PathVariable Long groupId, @RequestPart("avatar") MultipartFile avatar){
        if(groupId == null){
            return Result.error("群组ID不能为空");
        }

        // 上传群组头像
        String avatarUrl = groupService.uploadGroupAvatar(groupId, avatar);

        return Result.success("上传成功", avatarUrl);
    }

    // 更新群组信息
    @PutMapping("/profile/update/{groupId}")
    public Result<String> updateGroupProfile(@PathVariable Long groupId, @Valid @RequestBody GroupUpdateDTO groupUpdateDTO) {
        if (groupId == null) {
            return Result.error("群组ID不能为空");
        }

        return groupService.updateGroupProfile(groupId, groupUpdateDTO);

    }

    // 获取群组成员列表
    @GetMapping("/member/list/{groupId}")
    public Result<List<GroupMemberVO>> getGroupMemberList(@PathVariable Long groupId) {
        if (groupId == null) {
            return Result.error("群组ID不能为空");
        }

        List<GroupMemberVO> groupMemberVOList = groupService.getGroupMemberList(groupId);

        return Result.success("获取群组成员列表成功", groupMemberVOList);
    }

    //邀请用户加入群组
    @PostMapping("/invite/{groupId}")
    public Result<String> inviteUserToGroup(@Valid @RequestBody GroupMemberAddDTO groupMemberAddDTO) {
        groupService.inviteUsersToGroup(groupMemberAddDTO);

        return Result.success("邀请用户加入群组成功");
    }

    //移除群组成员（踢人）
    @DeleteMapping("/member/remove")
    public Result<String> deleteGroupMember(@Valid @RequestBody GroupMemberRemoveDTO groupMemberRemoveDTO) {
        groupService.deleteGroupMember(groupMemberRemoveDTO);

        return Result.success("删除群组成员成功");
    }

    // 退出群组
    @PostMapping("/exit/{groupId}")
    public Result<String> exitGroup(@PathVariable Long groupId) {
        if (groupId == null) {
            return Result.error("群组ID不能为空");
        }

        groupService.exitGroup(groupId, UserContext.getCurUserId());

        return Result.success("退出群组成功");
    }

    // 更新群组成员角色(升职为管理员或降级为普通成员)
    @PutMapping("/member/role")
    public Result<String> updateGroupMemberRole(@Valid @RequestBody GroupMemberRoleUpdateDTO groupMemberRoleUpdateDTO) {
        groupService.updateGroupMemberRole(groupMemberRoleUpdateDTO);

        return Result.success("更新群组成员角色成功");
    }

    //转让群主
    @PutMapping("/owner/transfer")
    public Result<String> transferGroupOwner(@Valid @RequestBody GroupTransferDTO groupTransferDTO) {
        groupService.transferGroupOwner(groupTransferDTO);

        return Result.success("转让群主成功");
    }

    // 解散群组
    @DeleteMapping("/dismiss/{groupId}")
    public Result<String> dismissGroup(@PathVariable Long groupId) {
        if (groupId == null) {
            return Result.error("群组ID不能为空");
        }

        groupService.dismissGroup(groupId);

        return Result.success("解散群组成功");
    }

    // 搜索群组
    @GetMapping("/search")
    public Result<List<GroupSearchVO>> searchGroups(@RequestParam String keyword){
        Long currentUserId = UserContext.getCurUserId();
        List<GroupSearchVO> groupSearchVOList = groupService.searchGroups(keyword, currentUserId);
        return Result.success("搜索群组成功", groupSearchVOList);
    }
}
