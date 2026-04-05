package com.minichat.space.controller;

import com.minichat.common.result.Result;
import com.minichat.space.dto.PublishSpaceCommentDTO;
import com.minichat.space.service.SpaceCommentService;
import com.minichat.space.vo.SpaceCommentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/space/comment")
@RequiredArgsConstructor
public class SpaceCommentController {

    private final SpaceCommentService spaceCommentService;

    // 发布空间评论
    @PostMapping("/publish")
    public Result<String> publishSpaceComment(@Valid @RequestBody PublishSpaceCommentDTO publishSpaceCommentDTO) {
        return spaceCommentService.publish(publishSpaceCommentDTO);
    }

    // 删除空间评论
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam Long commentId){
        return spaceCommentService.delete(commentId);
    }

    // 获取空间评论列表
    @GetMapping("/list")
    public Result<List<SpaceCommentVO>> list(@RequestParam Long postId){
        return spaceCommentService.list(postId);
    }
}
