package com.minichat.space.controller;

import com.minichat.common.exception.ValidationException;
import com.minichat.common.result.Result;
import com.minichat.space.dto.PublishSpacePostDTO;
import com.minichat.space.service.SpacePostService;
import com.minichat.space.vo.SpacePostVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/space/post")
@RequiredArgsConstructor
public class SpacePostController {

    private final SpacePostService spacePostService;

    @PostMapping("/publish")
    public Result<String> publish(@Valid @RequestBody PublishSpacePostDTO publishSpacePostDTO){
        spacePostService.publish(publishSpacePostDTO);
        return Result.success("发布成功");
    }

    @PostMapping("/upload/image")
    public Result<String> uploadImage(@RequestPart("file") MultipartFile file) {
        String url = spacePostService.uploadImage(file);
        return Result.success("上传成功", url);
    }

    @GetMapping("/list")
    public Result<List<SpacePostVO>> list(@RequestParam Long userId, @RequestParam Long friendId) {
        if(userId == null || friendId == null){
            throw new ValidationException("用户ID或好友ID不能为空");
        }
        List<SpacePostVO> list = spacePostService.list(userId, friendId);
        return Result.success(list);
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam Long postId){
        spacePostService.delete(postId);
        return Result.success("删除成功");
    }

    @PutMapping("/recover")
    public Result<String> recover(@RequestParam Long postId){
        spacePostService.recover(postId);
        return Result.success("恢复成功");
    }

    @GetMapping("/deleted/list")
    public Result<List<SpacePostVO>> deletedList(@RequestParam Long userId) {
        if(userId == null){
            throw new ValidationException("用户ID不能为空");
        }
        List<SpacePostVO> list = spacePostService.deletedList(userId);
        return Result.success(list);
    }

    @PostMapping("/change/like")
    public Result<String> changeLikeStatus(@RequestParam Long postId){
        if(postId == null){
            throw new ValidationException("帖子ID不能为空");
        }
        spacePostService.changeLikeStatus(postId);
        return Result.success("操作成功");
    }

}
