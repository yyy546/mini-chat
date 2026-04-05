package com.minichat.space.service;

import com.minichat.common.result.Result;
import com.minichat.space.dto.PublishSpaceCommentDTO;
import com.minichat.space.vo.SpaceCommentVO;
import jakarta.validation.Valid;
import java.util.List;

public interface SpaceCommentService {

    /**
     * 发布评论
     */
    Result<String> publish(@Valid PublishSpaceCommentDTO publishSpaceCommentDTO);

    /**
     * 删除评论
     */
    Result<String> delete(Long commentId);

    /**
     * 获取评论列表
     */
    Result<List<SpaceCommentVO>> list(Long postId);
}
