package com.minichat.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailVO {
    private Long id;
    private String nickname;
    private String avatar;
    private String gender;
    private String signature;
    private LocalDateTime createdTime;
}
