package com.minichat.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadCompleteResponseDTO {

    private String uploadId;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
}

