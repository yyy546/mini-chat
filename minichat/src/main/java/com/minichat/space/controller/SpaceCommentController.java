package com.minichat.space.controller;

import com.minichat.common.core.result.Result;
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

    @PostMapping("/publish")
    public Result<String> publishSpaceComment(@Valid @RequestBody PublishSpaceCommentDTO publishSpaceCommentDTO) {
        spaceCommentService.publish(publishSpaceCommentDTO);
        return Result.success("评论发布成功");
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam Long commentId){
        spaceCommentService.delete(commentId);
        return Result.success("评论删除成功");
    }

    @GetMapping("/list")
    public Result<List<SpaceCommentVO>> list(@RequestParam Long postId){
        List<SpaceCommentVO> list = spaceCommentService.list(postId);
        return Result.success(list);
    }
}
