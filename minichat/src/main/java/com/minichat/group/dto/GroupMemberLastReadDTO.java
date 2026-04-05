package com.minichat.group.dto;

import lombok.Data;

@Data
public class GroupMemberLastReadDTO {
    private Long groupId;
    private Long lastReadMessageId;
}
