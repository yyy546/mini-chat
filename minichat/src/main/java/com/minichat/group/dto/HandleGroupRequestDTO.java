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
public class HandleGroupRequestDTO {
    @NotNull(message = "群聊ID不能为空")
    private Long groupId;
    @NotNull(message = "申请人ID不能为空")
    private Long applicantId;
    @NotNull(message = "状态不能为空")
    private Integer status;
}
