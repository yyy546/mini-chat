package com.minichat.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("upload_task")
public class UploadTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String uploadId;
    private Long userId;
    private String bizType;
    private Long bizId;

    private String fileName;
    private Long fileSize;
    private String fileHash;

    private Integer chunkSize;
    private Integer totalChunks;
    private Integer uploadedChunks;

    private String ossUploadId;
    private String ossObjectKey;
    private String fileUrl;

    private Integer status;
    private String errorMsg;

    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
