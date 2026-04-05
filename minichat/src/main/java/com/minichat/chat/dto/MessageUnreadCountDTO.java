package com.minichat.chat.dto;

import lombok.Data;

@Data
public class MessageUnreadCountDTO {
    private Long friendId;
    private Long count;
}
