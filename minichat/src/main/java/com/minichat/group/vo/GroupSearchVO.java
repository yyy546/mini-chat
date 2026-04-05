package com.minichat.group.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupSearchVO {
    private Long id; // 被搜索群聊的ID（用于加入群聊）
    private String groupName; // 群聊名称（主要展示）
    private String avatar;
    private String announcement; // 群聊公告
    private Integer memberCount; // 群聊成员数量
    private Integer maxMembers; // 最大成员数量
    private Integer joinPolicy; // 加入策略（0自由加入/1需审批/2仅邀请）
}
