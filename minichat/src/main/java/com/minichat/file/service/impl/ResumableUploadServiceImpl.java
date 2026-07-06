package com.minichat.file.service.impl;

import com.aliyun.oss.model.PartETag;
import com.minichat.common.core.util.SnowflakeIdWorker;
import com.minichat.file.dto.UploadCompleteResponseDTO;
import com.minichat.file.dto.UploadInitRequestDTO;
import com.minichat.file.dto.UploadInitResponseDTO;
import com.minichat.file.dto.UploadStatusResponseDTO;
import com.minichat.file.entity.UploadPart;
import com.minichat.file.entity.UploadTask;
import com.minichat.file.mapper.UploadPartMapper;
import com.minichat.file.mapper.UploadTaskMapper;
import com.minichat.file.oss.OssMultipartClient;
import com.minichat.file.service.ResumableUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumableUploadServiceImpl implements ResumableUploadService {

    private static final int DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024;
    private static final long DEFAULT_EXPIRE_HOURS = 24;

    private final UploadTaskMapper uploadTaskMapper;
    private final UploadPartMapper uploadPartMapper;
    private final OssMultipartClient ossMultipartClient;
    private final SnowflakeIdWorker snowflakeIdWorker;
    @Value("${file.large-threshold-byte}")
    private Long largeThresholdByte;

    @Override
    public UploadInitResponseDTO initUploadTask(UploadInitRequestDTO request, Long userId) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (request.getFileSize() == null || request.getFileSize() <= 0) {
            throw new IllegalArgumentException("文件大小不合法");
        }
        long fileSize = request.getFileSize();
        if (largeThresholdByte != null && largeThresholdByte > 0 && fileSize < largeThresholdByte) {
            throw new IllegalArgumentException("文件过小，请使用普通上传接口");
        }
        String uploadId = String.valueOf(snowflakeIdWorker.nextId());
        int chunkSize = DEFAULT_CHUNK_SIZE;
        int totalChunks = (int) ((fileSize + chunkSize - 1) / chunkSize);
        String suffix = "";
        String fileName = request.getFileName();
        if (fileName != null) {
            int index = fileName.lastIndexOf(".");
            if (index != -1) {
                suffix = fileName.substring(index);
            }
        }
        String objectKey = "upload/" + userId + "/" + uploadId + suffix;
        String ossUploadId = ossMultipartClient.initiateMultipartUpload(objectKey);
        LocalDateTime expireTime = LocalDateTime.now().plusHours(DEFAULT_EXPIRE_HOURS);

        UploadTask uploadTask = UploadTask.builder()
                .uploadId(uploadId)
                .userId(userId)
                .bizType(request.getBizType())
                .bizId(request.getBizId())
                .fileName(fileName)
                .fileSize(fileSize)
                .fileHash(request.getFileHash())
                .chunkSize(chunkSize)
                .totalChunks(totalChunks)
                .uploadedChunks(0)
                .ossUploadId(ossUploadId)
                .ossObjectKey(objectKey)
                .status(0)
                .expireTime(expireTime)
                .build();
        uploadTaskMapper.insert(uploadTask);

        return UploadInitResponseDTO.builder()
                .uploadId(uploadId)
                .chunkSize(chunkSize)
                .totalChunks(totalChunks)
                .build();
    }

    @Override
    public void uploadChunk(String uploadId, Integer chunkIndex, MultipartFile file, Long userId) {
        if (uploadId == null || uploadId.isEmpty()) {
            throw new IllegalArgumentException("uploadId不能为空");
        }
        if (chunkIndex == null || chunkIndex <= 0) {
            throw new IllegalArgumentException("chunkIndex必须为正整数");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("分片文件不能为空");
        }
        UploadTask uploadTask = uploadTaskMapper.selectByUploadId(uploadId);
        if (uploadTask == null) {
            throw new IllegalArgumentException("上传任务不存在");
        }
        if (!uploadTask.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作该上传任务");
        }
        if (uploadTask.getStatus() == null || uploadTask.getStatus() != 0) {
            throw new IllegalStateException("上传任务状态异常");
        }
        try (InputStream inputStream = file.getInputStream()) {
            long size = file.getSize();
            String etag = ossMultipartClient.uploadPart(uploadTask.getOssObjectKey(), uploadTask.getOssUploadId(), chunkIndex, inputStream, size);
            UploadPart uploadPart = UploadPart.builder()
                    .uploadId(uploadId)
                    .chunkIndex(chunkIndex)
                    .chunkSize((int) size)
                    .etag(etag)
                    .status(1)
                    .retryTimes(0)
                    .build();
            uploadPartMapper.upsert(uploadPart);
            uploadTaskMapper.increaseUploadedChunks(uploadId);
        } catch (IOException e) {
            log.error("分片上传失败", e);
            uploadTaskMapper.updateStatusAndError(uploadId, 3, e.getMessage());
            throw new RuntimeException("分片上传失败");
        }
    }

    @Override
    public UploadStatusResponseDTO getUploadStatus(String uploadId, Long userId) {
        if (uploadId == null || uploadId.isEmpty()) {
            throw new IllegalArgumentException("uploadId不能为空");
        }
        UploadTask uploadTask = uploadTaskMapper.selectByUploadId(uploadId);
        if (uploadTask == null) {
            throw new IllegalArgumentException("上传任务不存在");
        }
        if (!uploadTask.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权查看该上传任务");
        }
        List<UploadPart> parts = uploadPartMapper.selectByUploadId(uploadId);
        List<Integer> uploadedIndexes = new ArrayList<>();
        if (parts != null && !parts.isEmpty()) {
            uploadedIndexes = parts.stream()
                    .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                    .map(UploadPart::getChunkIndex)
                    .collect(Collectors.toList());
        }
        return UploadStatusResponseDTO.builder()
                .uploadId(uploadId)
                .status(uploadTask.getStatus())
                .totalChunks(uploadTask.getTotalChunks())
                .uploadedChunks(uploadTask.getUploadedChunks())
                .fileSize(uploadTask.getFileSize())
                .fileName(uploadTask.getFileName())
                .fileUrl(uploadTask.getFileUrl())
                .uploadedChunkIndexList(uploadedIndexes)
                .build();
    }

    @Override
    public UploadCompleteResponseDTO completeUpload(String uploadId, Long userId) {
        if (uploadId == null || uploadId.isEmpty()) {
            throw new IllegalArgumentException("uploadId不能为空");
        }
        UploadTask uploadTask = uploadTaskMapper.selectByUploadId(uploadId);
        if (uploadTask == null) {
            throw new IllegalArgumentException("上传任务不存在");
        }
        if (!uploadTask.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权完成该上传任务");
        }
        if (uploadTask.getStatus() != null && uploadTask.getStatus() == 1) {
            return UploadCompleteResponseDTO.builder()
                    .uploadId(uploadId)
                    .fileUrl(uploadTask.getFileUrl())
                    .fileName(uploadTask.getFileName())
                    .fileSize(uploadTask.getFileSize())
                    .build();
        }
        List<UploadPart> parts = uploadPartMapper.selectByUploadId(uploadId);
        if (parts == null || parts.isEmpty()) {
            throw new IllegalStateException("没有找到已上传的分片");
        }
        List<PartETag> partETags = parts.stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                .map(p -> new PartETag(p.getChunkIndex(), p.getEtag()))
                .collect(Collectors.toList());
        if (partETags.isEmpty()) {
            throw new IllegalStateException("没有可用于合并的分片");
        }
        String fileUrl;
        try {
            fileUrl = ossMultipartClient.completeMultipartUpload(uploadTask.getOssObjectKey(), uploadTask.getOssUploadId(), partETags);
            uploadTaskMapper.updateStatusAndResult(uploadId, 1, uploadTask.getOssObjectKey(), fileUrl);
        } catch (Exception e) {
            log.error("完成分片上传失败", e);
            uploadTaskMapper.updateStatusAndError(uploadId, 3, e.getMessage());
            throw new RuntimeException("完成分片上传失败");
        }
        return UploadCompleteResponseDTO.builder()
                .uploadId(uploadId)
                .fileUrl(fileUrl)
                .fileName(uploadTask.getFileName())
                .fileSize(uploadTask.getFileSize())
                .build();
    }
}
