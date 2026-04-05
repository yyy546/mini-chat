package com.minichat.friend.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendDetailVO {
    private Long friendUserId;
    private String friendAvatar;
    private String remarkName;
    private String friendNickname;
    private String groupName;
    private String gender;
    private String signature;
}
