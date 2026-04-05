package com.minichat.file.task;

import com.minichat.file.entity.UploadTask;
import com.minichat.file.mapper.UploadPartMapper;
import com.minichat.file.mapper.UploadTaskMapper;
import com.minichat.file.oss.OssMultipartClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class UploadCleanupTask {

    private static final int CLEANUP_BATCH_SIZE = 100;

    private final UploadTaskMapper uploadTaskMapper;
    private final UploadPartMapper uploadPartMapper;
    private final OssMultipartClient ossMultipartClient;

    @Scheduled(cron = "0 0/30 * * * ?")
    public void cleanupExpiredUploadTasks() {
        LocalDateTime now = LocalDateTime.now();
        log.info("开始清理过期的分片上传任务，当前时间：{}", now);

        while (true) {
            List<UploadTask> tasks = uploadTaskMapper.selectExpiredUnfinished(now, CLEANUP_BATCH_SIZE);
            if (tasks == null || tasks.isEmpty()) {
                log.info("暂无需要清理的过期分片上传任务");
                break;
            }

            for (UploadTask task : tasks) {
                String uploadId = task.getUploadId();
                String objectKey = task.getOssObjectKey();
                String ossUploadId = task.getOssUploadId();
                try {
                    if (objectKey != null && ossUploadId != null) {
                        try {
                            ossMultipartClient.abortMultipartUpload(objectKey, ossUploadId);
                            log.info("已终止OSS分片上传，uploadId={}, objectKey={}", uploadId, objectKey);
                        } catch (Exception e) {
                            log.error("终止OSS分片上传失败，uploadId={}, objectKey={}", uploadId, objectKey, e);
                        }
                    }
                    uploadPartMapper.deleteByUploadId(uploadId);
                    uploadTaskMapper.deleteById(task.getId());
                    log.info("已清理过期上传任务，uploadId={}", uploadId);
                } catch (Exception e) {
                    log.error("清理过期上传任务失败，uploadId={}", uploadId, e);
                }
            }

            if (tasks.size() < CLEANUP_BATCH_SIZE) {
                log.info("本轮分片上传任务清理完成，本次清理数量：{}", tasks.size());
                break;
            }
        }
    }
}

