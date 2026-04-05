package com.minichat.friend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandleFriendRequestDTO {
    @NotNull(message = "好友申请ID不能为空")
    private Long requestId;
    @NotNull(message = "处理状态不能为空")
    private Integer status;
}
