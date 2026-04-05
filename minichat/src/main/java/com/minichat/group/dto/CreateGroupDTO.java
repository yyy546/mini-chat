package com.minichat.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupDTO {
    @NotBlank(message = "群组名称不能为空")
    private String groupName;    // 群组名称
    private String announcement;    // 群组公告
    @Range(min = 3, max = 200, message = "最大成员数量必须在1-200之间")
    private Integer maxMembers;    // 最大成员数量
    @NotNull(message = "加入策略不能为空")
    @Range(min = 0, max = 2, message = "加入策略必须在0-2之间")
    private Integer joinPolicy;    // 加入策略（0自由加入/1需审批/2仅邀请）
    @NotNull(message = "邀请策略不能为空")
    @Range(min = 0, max = 2, message = "邀请策略必须在0-2之间")
    private Integer invitePolicy;    // 邀请策略（0所有成员/1管理员/2群主）
    private List<Long> memberIds;    // 成员ID列表
}
