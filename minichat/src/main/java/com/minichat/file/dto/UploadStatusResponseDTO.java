package com.minichat.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadStatusResponseDTO {

    private String uploadId;
    private Integer status;
    private Integer totalChunks;
    private Integer uploadedChunks;
    private Long fileSize;
    private String fileName;
    private String fileUrl;
    private List<Integer> uploadedChunkIndexList;
}

