package com.minichat.group.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupRequest {
    private Long id;    // 主键ID
    private Long groupId;    // 所属群聊ID
    private Long applicantId;    // 申请人ID
    private Long reviewerId;    // 审核人ID
    private String message;    // 申请消息
    private Integer status;    // 申请状态（0：待处理，1：已通过，2：已拒绝）
    private LocalDateTime createdTime;    // 创建时间
    private LocalDateTime processedTime;    // 处理时间
}
