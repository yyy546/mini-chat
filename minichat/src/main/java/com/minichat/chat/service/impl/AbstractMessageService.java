package com.minichat.chat.service.impl;

import com.minichat.chat.vo.FileVO;
import com.minichat.common.constants.MessageConstants;
import com.minichat.common.result.Result;
import com.minichat.common.util.OssFileUtil;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.function.Consumer;

@Slf4j
public abstract class AbstractMessageService {

    @Resource
    protected OssFileUtil ossFileUtil;

    /**
     * 校验文件信息
     * @param fileUrl 文件URL
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @return 是否合法
     */
    protected boolean validateFile(String fileUrl, String fileName, Long fileSize){
        // 图片/文件消息：校验文件信息
        if (StringUtils.isEmpty(fileUrl)) {
            return false;
        }
        if (StringUtils.isEmpty(fileName)) {
            return false;
        }
        if (fileSize == null || fileSize <= 0) {
            return false;
        }

        return true;
    }

    /**
     * 上传文件公共方法
     * @param file 上传的文件
     * @param imagePath 图片存储路径
     * @param filePath 文件存储路径
     * @return 文件VO
     */
    protected Result<FileVO> uploadFileCommon(MultipartFile file, String imagePath, String filePath){
        try{
            // 提取文件扩展名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            }

            // 根据文件扩展名和MIME类型自动判断是否为图片
            boolean isImage = OssFileUtil.isImageFile(fileExtension, file.getContentType());

            // 根据实际文件类型决定存储路径和消息类型
            String storagePath = isImage ? imagePath : filePath;
            Integer actualMessageType = isImage ? MessageConstants.IMAGE : MessageConstants.FILE;  // 2=图片，3=文件

            // 调用OSS工具类上传
            String fileUrl = ossFileUtil.uploadFile(file, storagePath);

            // 构建返回结果
            FileVO fileVO = FileVO.builder()
                    .fileUrl(fileUrl)
                    .fileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .fileType(isImage ? "image" : "file")
                    .fileExtension(fileExtension)
                    .messageType(actualMessageType)  // 返回实际的消息类型
                    .build();

            return Result.success(fileVO);
        }catch(IOException e){
            log.error("文件上传失败", e);
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    protected <T> Boolean processRecallMessage(LocalDateTime sendTime, T message, Consumer<T> updateAndSaveMethod){
        LocalDateTime now = LocalDateTime.now();
        // 校验撤回时间限制
        if (now.isAfter(sendTime.plusMinutes(MessageConstants.MESSAGE_RECALL_TIME_LIMIT))) {
            return false;
        }

        // 更新消息为已撤回状态
        updateAndSaveMethod.accept(message);
        return true;
    }
}
