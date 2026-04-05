package com.minichat.group.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupVO {
    private Long id;
    private String groupName;
    private String avatar;
    private String announcement;
    private Long creatorId;
    private Long ownerId;
    private Integer memberCount;
    private Integer maxMembers;
    private Integer joinPolicy;
    private Integer invitePolicy;
    private LocalDateTime createdTime;

    private List<Long> adminIds;
}
