package com.minichat.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchVO {
    private Long id; // 被搜索用户的ID（用于发送好友申请）
    private String username; // 用户名（辅助识别）
    private String nickname; // 昵称（主要展示）
    private String avatar;
    private Integer status;
}
