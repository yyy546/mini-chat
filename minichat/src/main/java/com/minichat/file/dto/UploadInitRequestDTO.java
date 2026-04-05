package com.minichat.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadInitRequestDTO {

    private String fileName;
    private Long fileSize;
    private String fileHash;    // 文件摘要（如MD5/SHA256），用于秒传/幂等
    private String bizType;     // 业务类型
    private Long bizId;     // 业务ID
}

