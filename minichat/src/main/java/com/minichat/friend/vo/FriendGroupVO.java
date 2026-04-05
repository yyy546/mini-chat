package com.minichat.friend.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendGroupVO {
    private String groupName;
    private Integer friendCount;
    private Integer totalCount;
}
