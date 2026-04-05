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
public class GroupTransferDTO {
    @NotNull(message = "群组ID不能为空")
    private Long groupId;
    @NotNull(message = "新群主ID不能为空")
    private Long newOwnerId;
}
