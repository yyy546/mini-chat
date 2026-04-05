package com.minichat.space.controller;

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

    // 发布空间帖子
    @PostMapping("/publish")
    public Result<String> publish(@Valid @RequestBody PublishSpacePostDTO publishSpacePostDTO){
        return spacePostService.publish(publishSpacePostDTO);
    }

    // 上传图片
    @PostMapping("/upload/image")
    public Result<String> uploadImage(@RequestPart("file") MultipartFile file) {
        return spacePostService.uploadImage(file);
    }

    // 获取某个好友的空间帖子列表
    @GetMapping("/list")
    public Result<List<SpacePostVO>> list(@RequestParam Long userId, @RequestParam Long friendId) {
        if(userId == null || friendId == null){
            return Result.error("用户ID或好友ID不能为空");
        }
        return spacePostService.list(userId, friendId);
    }

    //删除空间帖子
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam Long postId){
        return spacePostService.delete(postId);
    }

    //恢复空间帖子
    @PutMapping("/recover")
    public Result<String> recover(@RequestParam Long postId){
        return spacePostService.recover(postId);
    }

    //查看已删除的空间帖子列表
    @GetMapping("/deleted/list")
    public Result<List<SpacePostVO>> deletedList(@RequestParam Long userId) {
        if(userId == null){
            return Result.error("用户ID不能为空");
        }
        return spacePostService.deletedList(userId);
    }

    //切换点赞状态
    @PostMapping("/change/like")
    public Result<String> changeLikeStatus(@RequestParam Long postId){
        if(postId == null){
            return Result.error("帖子ID不能为空");
        }
        return spacePostService.changeLikeStatus(postId);
    }

}
