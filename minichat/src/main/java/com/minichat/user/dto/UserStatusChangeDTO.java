package com.minichat.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusChangeDTO {
    private Long userId;
    private String nickname;
    private Boolean isOnline;
}
