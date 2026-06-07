package com.minichat.chat.service.impl;

import com.minichat.chat.vo.FileVO;
import com.minichat.common.constants.MessageConstants;
import com.minichat.common.exception.FileException;
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

    protected boolean validateFile(String fileUrl, String fileName, Long fileSize){
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

    protected FileVO uploadFileCommon(MultipartFile file, String imagePath, String filePath){
        try{
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            }

            boolean isImage = OssFileUtil.isImageFile(fileExtension, file.getContentType());

            String storagePath = isImage ? imagePath : filePath;
            Integer actualMessageType = isImage ? MessageConstants.IMAGE : MessageConstants.FILE;

            String fileUrl = ossFileUtil.uploadFile(file, storagePath);

            return FileVO.builder()
                    .fileUrl(fileUrl)
                    .fileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .fileType(isImage ? "image" : "file")
                    .fileExtension(fileExtension)
                    .messageType(actualMessageType)
                    .build();
        }catch(IOException e){
            log.error("文件上传失败", e);
            throw new FileException("文件上传失败：" + e.getMessage());
        }
    }

    protected <T> Boolean processRecallMessage(LocalDateTime sendTime, T message, Consumer<T> updateAndSaveMethod){
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(sendTime.plusMinutes(MessageConstants.MESSAGE_RECALL_TIME_LIMIT))) {
            return false;
        }

        updateAndSaveMethod.accept(message);
        return true;
    }
}
