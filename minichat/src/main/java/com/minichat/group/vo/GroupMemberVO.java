package com.minichat.group.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberVO {
    private Long userId;
    private String avatar;
    private String nickname;
    private String nicknameInGroup;    // 群内昵称
    private Integer role;    // 角色（0普通成员/1管理员/2群主）
    private Integer isMuted;    // 是否被禁言（0正常/1禁言）
    private LocalDateTime joinTime;    // 加入时间
}
