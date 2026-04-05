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
@TableName("upload_part")
public class UploadPart {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String uploadId;
    private Integer chunkIndex;
    private Integer chunkSize;

    private String etag;
    private Integer status;
    private Integer retryTimes;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

