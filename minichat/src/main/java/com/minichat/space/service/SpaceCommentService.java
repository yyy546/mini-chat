package com.minichat.space.service;

import com.minichat.space.dto.PublishSpaceCommentDTO;
import com.minichat.space.vo.SpaceCommentVO;
import jakarta.validation.Valid;
import java.util.List;

public interface SpaceCommentService {

    void publish(@Valid PublishSpaceCommentDTO publishSpaceCommentDTO);

    void delete(Long commentId);

    List<SpaceCommentVO> list(Long postId);
}
