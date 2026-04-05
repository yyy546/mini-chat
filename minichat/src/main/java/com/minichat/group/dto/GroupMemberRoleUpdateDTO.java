package com.minichat.group.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberRoleUpdateDTO {
    @NotNull(message = "群组ID不能为空")
    private Long groupId;
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    @NotNull(message = "角色不能为空")
    private Integer role;   // 角色：0-普通成员，1-管理员
}
