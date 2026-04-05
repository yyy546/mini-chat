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
public class GroupMember {
    private Long id;    // 关系ID
    private Long groupId;    // 群组ID
    private Long userId;    // 用户ID
    private Integer role;    // 角色（0普通成员/1管理员/2群主）
    private Integer isMuted;    // 是否被禁言（0正常/1禁言）
    private Integer isDeleted;    // 是否删除（0正常/1退出）
    private String nicknameInGroup;    // 群内昵称
    private Long lastReadMessageId;    // 最后读取的消息ID
    private LocalDateTime joinTime;    // 加入时间
}
