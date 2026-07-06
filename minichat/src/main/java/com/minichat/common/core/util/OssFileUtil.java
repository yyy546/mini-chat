package com.minichat.common.core.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 阿里云OSS通用文件操作工具类
 * 支持任意类型文件（头像、文档、图片、视频等）的上传、删除、存在性校验、元信息获取等操作
 */
@Slf4j
@Component
public class OssFileUtil implements DisposableBean {

    // OSS基础配置
    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    // 默认文件存储路径（比如头像默认路径，可配置）
    @Value("${aliyun.oss.default-path:default/}")
    private String defaultPath;

    // 懒加载创建OSS客户端（单例，线程安全）
    private volatile OSS ossClient;

    /**
     * 获取OSS客户端（单例模式，避免频繁创建/销毁）
     */
    private OSS getOssClient() {
        if (ossClient == null) {
            synchronized (OssFileUtil.class) {
                if (ossClient == null) {
                    ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
                }
            }
        }
        return ossClient;
    }

    /**
     * 通用文件上传（默认存储路径）
     * @param file 待上传的文件（MultipartFile）
     * @return 文件的公开访问URL
     * @throws IOException 文件流异常
     */
    public String uploadFile(MultipartFile file) throws IOException {
        return uploadFile(file, defaultPath);
    }

    /**
     * 通用文件上传（自定义存储路径）
     * @param file 待上传的文件（MultipartFile）
     * @param filePath 自定义存储路径（比如：avatar/、document/、video/）
     * @return 文件的公开访问URL
     * @throws IOException 文件流异常
     */
    public String uploadFile(MultipartFile file, String filePath) throws IOException {
        // 校验文件
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        // 1. 处理文件路径（确保路径以/结尾）
        String finalFilePath = filePath.endsWith("/") ? filePath : filePath + "/";

        // 2. 生成唯一文件名（避免重复，保留原文件后缀）
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.lastIndexOf(".") == -1) {
            throw new IllegalArgumentException("文件名称不合法，缺少后缀");
        }
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFileName = UUID.randomUUID().toString() + suffix;

        // 3. 拼接最终的OSS文件Key（路径+文件名）
        String fileKey = finalFilePath + uniqueFileName;

        // 4. 上传文件到OSS
        try (InputStream inputStream = file.getInputStream()) {
            // 设置文件元信息（可选，比如Content-Type）
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            getOssClient().putObject(bucketName, fileKey, inputStream, metadata);

            // 5. 生成公开访问URL（Bucket需配置公共读权限）
            return String.format("https://%s.%s/%s", bucketName, endpoint, fileKey);
        } catch (OSSException e) {
            throw new RuntimeException("OSS文件上传失败：" + e.getMessage(), e);
        }
    }

    /**
     * 删除OSS上的文件（通过文件URL）
     * @param fileUrl 文件的OSS访问URL
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        // 解析URL中的文件Key（核心优化：正确提取完整路径）
        String fileKey = parseFileKeyFromUrl(fileUrl);
        if (fileKey == null) {
            throw new IllegalArgumentException("OSS文件URL格式非法：" + fileUrl);
        }
        // 强行跳过默认头像的删除
        if ("avatar/default_avatar.png".equals(fileKey)) {
            log.warn("尝试删除系统默认头像，已阻止！ObjectKey：{}", fileKey);
            return;
        }

        // 删除文件
        try {
            getOssClient().deleteObject(bucketName, fileKey);
        } catch (OSSException e) {
            throw new RuntimeException("OSS文件删除失败：" + e.getMessage(), e);
        }
    }

    /**
     * 判断OSS上的文件是否存在
     * @param fileKey 文件在OSS中的完整Key（比如：avatar/123-uuid.jpg）
     * @return true-存在，false-不存在
     */
    public boolean isFileExist(String fileKey) {
        try {
            return getOssClient().doesObjectExist(bucketName, fileKey);
        } catch (OSSException e) {
            throw new RuntimeException("校验OSS文件是否存在失败：" + e.getMessage(), e);
        }
    }

    /**
     * 获取OSS文件的元信息（比如文件大小、ContentType等）
     * @param fileKey 文件在OSS中的完整Key
     * @return 文件元信息
     */
    public ObjectMetadata getFileMetadata(String fileKey) {
        try (OSSObject ossObject = getOssClient().getObject(bucketName, fileKey)) {
            return ossObject.getObjectMetadata();
        } catch (OSSException | IOException e) {
            throw new RuntimeException("获取OSS文件元信息失败：" + e.getMessage(), e);
        }
    }

    /**
     * 从OSS URL中解析文件Key（核心修复：解决原逻辑路径解析错误问题）
     * 示例URL：https://bucket.oss-cn-shanghai.aliyuncs.com/avatar/123.jpg
     * 解析结果：avatar/123.jpg
     */
    private String parseFileKeyFromUrl(String fileUrl) {
        String domain = bucketName + "." + endpoint;
        int domainIndex = fileUrl.indexOf(domain);
        if (domainIndex == -1) {
            return null;
        }
        // 截取domain后的部分（去掉/）
        return fileUrl.substring(domainIndex + domain.length() + 1);
    }

    /**
     * 销毁OSS客户端（Spring容器销毁时调用，释放资源）
     */
    @PreDestroy
    @Override
    public void destroy() {
        // 释放OSS客户端资源（Spring容器关闭时确保执行）
        if (ossClient != null) {
            try {
                ossClient.shutdown();
                log.info("OSS客户端资源已成功释放");
            } catch (Exception e) {
                log.error("释放OSS客户端资源失败：{}", e.getMessage());
            } finally {
                ossClient = null;
            }
        }
    }

    /**
     * 根据文件扩展名和MIME类型判断是否为图片文件
     * @param fileExtension 文件扩展名（小写）
     * @param contentType MIME类型
     * @return true表示是图片，false表示不是图片
     */
    public static boolean isImageFile(String fileExtension, String contentType) {
        // 支持的图片扩展名
        Set<String> imageExtensions = new HashSet<>(Arrays.asList(
                "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "tiff", "tif"
        ));

        // 支持的图片MIME类型
        Set<String> imageMimeTypes = new HashSet<>(Arrays.asList(
                "image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp",
                "image/webp", "image/svg+xml", "image/x-icon", "image/tiff", "image/x-tiff"
        ));

        // 优先根据扩展名判断
        if (fileExtension != null && !fileExtension.isEmpty()) {
            if (imageExtensions.contains(fileExtension.toLowerCase())) {
                return true;
            }
        }

        // 如果扩展名无法判断，则根据MIME类型判断
        if (contentType != null && !contentType.isEmpty()) {
            String lowerContentType = contentType.toLowerCase();
            if (imageMimeTypes.contains(lowerContentType)) {
                return true;
            }
            // 也支持以image/开头的MIME类型
            return lowerContentType.startsWith("image/");
        }

        return false;
    }

}
