package com.minichat.group.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberAddDTO {
    @NotNull(message = "群组ID不能为空")
    private Long groupId;

    @NotNull(message = "成员列表不能为空")
    @Size(min = 1, message = "至少邀请一名成员")
    private List<Long> userIds;
}
