package com.minichat.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginVO {
    // 用户基本信息（非敏感）
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private Integer gender;
    private String signature;

    // 登录令牌
    private String token;
}
