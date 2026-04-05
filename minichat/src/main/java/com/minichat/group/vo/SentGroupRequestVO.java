package com.minichat.group.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentGroupRequestVO {
    private Long groupId; // 群聊ID
    private String groupName;
    private String avatar; // 群聊头像
    private Long applicantId; // 申请人ID
    private String message; // 申请留言
    private LocalDateTime createdTime; // 申请时间
    private Integer status; // 申请状态（0-待处理、1-已同意、2-已拒绝）
}
