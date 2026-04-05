package com.minichat.file.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.AbortMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class OssMultipartClient {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    private volatile OSS ossClient;

    private OSS getOssClient() {
        if (ossClient == null) {
            synchronized (OssMultipartClient.class) {
                if (ossClient == null) {
                    ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
                }
            }
        }
        return ossClient;
    }

    public String initiateMultipartUpload(String objectKey) {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, objectKey);
        InitiateMultipartUploadResult result = getOssClient().initiateMultipartUpload(request);
        return result.getUploadId();
    }

    public String uploadPart(String objectKey, String uploadId, int partNumber, InputStream inputStream, long partSize) {
        UploadPartRequest request = new UploadPartRequest();
        request.setBucketName(bucketName);
        request.setKey(objectKey);
        request.setUploadId(uploadId);
        request.setInputStream(inputStream);
        request.setPartSize(partSize);
        request.setPartNumber(partNumber);
        UploadPartResult result = getOssClient().uploadPart(request);
        return result.getETag();
    }

    public String completeMultipartUpload(String objectKey, String uploadId, List<PartETag> partETags) {
        if (partETags == null || partETags.isEmpty()) {
            throw new IllegalArgumentException("partETags不能为空");
        }
        Collections.sort(partETags, Comparator.comparingInt(PartETag::getPartNumber));
        CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(bucketName, objectKey, uploadId, partETags);
        getOssClient().completeMultipartUpload(request);
        return String.format("https://%s.%s/%s", bucketName, endpoint, objectKey);
    }

    public void abortMultipartUpload(String objectKey, String uploadId) {
        AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(bucketName, objectKey, uploadId);
        getOssClient().abortMultipartUpload(request);
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            try {
                ossClient.shutdown();
            } catch (OSSException e) {
                log.error("关闭OSS分片客户端失败", e);
            } finally {
                ossClient = null;
            }
        }
    }
}

