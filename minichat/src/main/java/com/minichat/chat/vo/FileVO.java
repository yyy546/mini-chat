package com.minichat.chat.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileVO{
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String fileExtension;
    private Integer messageType;  // 实际的消息类型：1=文本，2=图片，3=文件
}
