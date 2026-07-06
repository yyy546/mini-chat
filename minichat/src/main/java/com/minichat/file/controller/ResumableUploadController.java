package com.minichat.file.controller;

import com.minichat.common.core.result.Result;
import com.minichat.common.security.jwt.UserContext;
import com.minichat.file.dto.UploadCompleteResponseDTO;
import com.minichat.file.dto.UploadInitRequestDTO;
import com.minichat.file.dto.UploadInitResponseDTO;
import com.minichat.file.dto.UploadStatusResponseDTO;
import com.minichat.file.service.ResumableUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class ResumableUploadController {

    private final ResumableUploadService resumableUploadService;

    @PostMapping("/init")
    public Result<UploadInitResponseDTO> init(@RequestBody UploadInitRequestDTO request) {
        Long userId = UserContext.getCurUserId();
        UploadInitResponseDTO response = resumableUploadService.initUploadTask(request, userId);
        return Result.success(response);
    }

    @PostMapping("/chunk")
    public Result<String> uploadChunk(@RequestParam("uploadId") String uploadId,
                                      @RequestParam("chunkIndex") Integer chunkIndex,
                                      @RequestPart("file") MultipartFile file) {
        Long userId = UserContext.getCurUserId();
        resumableUploadService.uploadChunk(uploadId, chunkIndex, file, userId);
        return Result.success("分片上传成功");
    }

    @GetMapping("/status")
    public Result<UploadStatusResponseDTO> status(@RequestParam("uploadId") String uploadId) {
        Long userId = UserContext.getCurUserId();
        UploadStatusResponseDTO status = resumableUploadService.getUploadStatus(uploadId, userId);
        return Result.success(status);
    }

    @PostMapping("/complete")
    public Result<UploadCompleteResponseDTO> complete(@RequestParam("uploadId") String uploadId) {
        Long userId = UserContext.getCurUserId();
        UploadCompleteResponseDTO result = resumableUploadService.completeUpload(uploadId, userId);
        return Result.success(result);
    }
}
