package com.minichat.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadInitResponseDTO {

    private String uploadId;
    private Integer chunkSize;
    private Integer totalChunks;
}

