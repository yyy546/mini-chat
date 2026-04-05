package com.minichat.friend.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendVO {
    private Long id;
    private Long friendId;
    private String nickname;
    private String remarkName;      //备注名
    private String avatar;
    private String groupName;      //分组名  默认分组为"我的好友"
}
