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
public class ReceivedGroupRequestVO {
    private Long requestId; // 申请ID
    private Long groupId; // 群聊ID
    private String groupName; // 群聊名称
    private Long applicantId; // 申请人ID
    private String applicantName; // 申请人昵称
    private String applicantAvatar; // 申请人头像
    private String message; // 申请留言
    private LocalDateTime createdTime; // 申请时间
    private Integer status; // 申请状态（0-待处理、1-已同意、2-已拒绝）
}
