package com.minichat.file.service;

import com.minichat.file.dto.UploadCompleteResponseDTO;
import com.minichat.file.dto.UploadInitRequestDTO;
import com.minichat.file.dto.UploadInitResponseDTO;
import com.minichat.file.dto.UploadStatusResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface ResumableUploadService {

    /**
     * 初始化上传任务
     * @param request
     * @param userId
     * @return
     */
    UploadInitResponseDTO initUploadTask(UploadInitRequestDTO request, Long userId);

    /**
     * 上传分块
     * @param uploadId
     * @param chunkIndex
     * @param file
     * @param userId
     */
    void uploadChunk(String uploadId, Integer chunkIndex, MultipartFile file, Long userId);

    /**
     * 获取上传状态
     * @param uploadId
     * @param userId
     * @return
     */
    UploadStatusResponseDTO getUploadStatus(String uploadId, Long userId);

    /**
     * 完成上传
     * @param uploadId
     * @param userId
     * @return
     */
    UploadCompleteResponseDTO completeUpload(String uploadId, Long userId);
}

