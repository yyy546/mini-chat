package com.minichat.chat.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PrivateMessageVO extends BaseMessageVO {
    private Long receiverId;
    private String receiverNickname;
    private Integer isRead;     // 是否已读
}
