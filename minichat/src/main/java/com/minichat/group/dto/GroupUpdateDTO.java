package com.minichat.group.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupUpdateDTO {
    @NotNull(message = "群聊名称不能为空")
    private String groupName;
    private String avatar;
    private String announcement;
    @Range(min = 1, max = 500, message = "最大成员数必须在1-500之间")
    private Integer maxMembers;
    @Range(min = 0, max = 2, message = "加入策略必须在0-2之间")
    private Integer joinPolicy;
    @Range(min = 0, max = 2, message = "邀请策略必须在0-2之间")
    private Integer invitePolicy;
}
