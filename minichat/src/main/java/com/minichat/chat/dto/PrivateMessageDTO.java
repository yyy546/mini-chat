package com.minichat.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivateMessageDTO {
    private Long senderId;      //发送者ID
    @NotNull(message = "接收方ID不能为空")
    @Positive(message = "接收方ID必须为正整数")
    private Long receiverId;    //接收者ID
    @Range(min = 1, max = 4, message = "消息类型必须在1-4之间")
    private Integer messageType;    //消息类型 1:文本 2:图片 3:文件 4：系统消息
    private String content;     //消息内容
    private String fileUrl;     //文件URL
    private String fileName;    //文件名
    private Long fileSize;      //文件大小

    private String tempId;      //临时ID，用于消息去重
}
