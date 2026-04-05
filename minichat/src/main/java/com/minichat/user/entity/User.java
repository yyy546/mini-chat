package com.minichat.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;    //用户ID
    private String username;    //用户名
    private String password;    //密码
    private String nickname;    //昵称
    private String avatar;    //头像URL
    private Integer gender;     //0:未知 1:男 2:女
    private String signature;    //签名
    private Integer status;     //0:正常 1:禁用
    private LocalDateTime lastLoginTime;    //最后登录时间
    private LocalDateTime createdTime;    //创建时间
    private LocalDateTime updatedTime;    //更新时间
}
