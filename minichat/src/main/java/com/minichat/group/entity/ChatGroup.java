package com.minichat.group.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatGroup {
    private Long id;    // 群组ID
    private String groupName;    // 群组名称
    private String avatar;    // 群组头像
    private String announcement;    // 群组公告
    private Long creatorId;    // 创建者ID
    private Long ownerId;    // 群主ID
    private Integer memberCount;    // 成员数量
    private Integer maxMembers;    // 最大成员数量
    private Integer joinPolicy;    // 加入策略（0自由加入/1需审批/2仅邀请）
    private Integer invitePolicy;    // 邀请策略（0所有成员/1管理员/2群主）
    private Integer isDeleted;    // 是否删除（0正常/1已删除）
    private LocalDateTime createdTime;    // 创建时间
    private LocalDateTime updatedTime;    // 更新时间
}
