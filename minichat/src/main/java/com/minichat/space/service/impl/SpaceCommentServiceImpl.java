package com.minichat.space.service.impl;

import com.minichat.space.constants.SpaceConstants;
import com.minichat.common.core.exception.ErrorCode;
import com.minichat.common.core.exception.SpaceException;
import com.minichat.common.security.jwt.UserContext;
import com.minichat.space.dto.PublishSpaceCommentDTO;
import com.minichat.space.entity.SpaceComment;
import com.minichat.space.entity.SpacePost;
import com.minichat.space.mapper.SpaceCommentMapper;
import com.minichat.space.mapper.SpacePostMapper;
import com.minichat.space.service.SpaceCommentService;
import com.minichat.space.vo.SpaceCommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpaceCommentServiceImpl implements SpaceCommentService {

    private final SpacePostMapper spacePostMapper;
    private final SpaceCommentMapper spaceCommentMapper;

    @Override
    public void publish(PublishSpaceCommentDTO publishSpaceCommentDTO) {
        Long currentUserId = UserContext.getCurUserId();
        if (!currentUserId.equals(publishSpaceCommentDTO.getPublishId())) {
            throw new SpaceException(ErrorCode.POST_PERMISSION_DENIED, "发布人ID与当前用户ID不一致");
        }

        SpacePost spacePost = spacePostMapper.selectById(publishSpaceCommentDTO.getPostId());
        if (spacePost == null || SpaceConstants.DISABLE_STATUS.equals(spacePost.getStatus())) {
            throw new SpaceException(ErrorCode.POST_NOT_FOUND, "帖子不存在或已被删除");
        }

        SpaceComment spaceComment = SpaceComment.builder()
                .postId(publishSpaceCommentDTO.getPostId())
                .publishId(publishSpaceCommentDTO.getPublishId())
                .content(publishSpaceCommentDTO.getContent())
                .status(SpaceConstants.NORMAL_STATUS)
                .createdTime(LocalDateTime.now())
                .build();
        spaceCommentMapper.insert(spaceComment);

        spacePostMapper.updateAddCommentsCountById(spacePost);
    }

    @Override
    public void delete(Long commentId) {
        SpaceComment spaceComment = spaceCommentMapper.selectById(commentId);
        if (spaceComment == null || SpaceConstants.DISABLE_STATUS.equals(spaceComment.getStatus())) {
            throw new SpaceException(ErrorCode.COMMENT_NOT_FOUND, "评论不存在或已被删除");
        }
        if (!spaceComment.getPublishId().equals(UserContext.getCurUserId())) {
            throw new SpaceException(ErrorCode.POST_PERMISSION_DENIED, "非评论发布人，不能删除");
        }
        spaceComment.setStatus(SpaceConstants.DISABLE_STATUS);
        spaceCommentMapper.updateDisableStatusById(spaceComment);

        SpacePost spacePost = spacePostMapper.selectById(spaceComment.getPostId());
        spacePostMapper.updateSubCommentsCountById(spacePost);
    }

    @Override
    public List<SpaceCommentVO> list(Long postId) {
        List<SpaceCommentVO> commentList = spaceCommentMapper.selectListByPostId(postId);
        return commentList;
    }
}
