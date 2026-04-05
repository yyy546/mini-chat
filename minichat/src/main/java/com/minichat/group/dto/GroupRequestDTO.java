package com.minichat.group.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupRequestDTO {
    @NotNull(message = "群ID不能为空")
    private Long groupId;       //加入的群ID
    private String message;      //加入群的消息
}
