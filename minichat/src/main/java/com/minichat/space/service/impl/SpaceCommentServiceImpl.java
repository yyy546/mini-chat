package com.minichat.space.service.impl;

import com.minichat.common.constants.SpaceConstants;
import com.minichat.common.result.Result;
import com.minichat.common.util.UserContext;
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
    public Result<String> publish(PublishSpaceCommentDTO publishSpaceCommentDTO) {
        Long currentUserId = UserContext.getCurUserId();
        if(!currentUserId.equals(publishSpaceCommentDTO.getPublishId())){
            return Result.error("发布人ID与当前用户ID不一致");
        }

        SpacePost spacePost = spacePostMapper.selectById(publishSpaceCommentDTO.getPostId());
        if(spacePost == null || SpaceConstants.DISABLE_STATUS.equals(spacePost.getStatus())){
            return Result.error("帖子不存在或已被删除");
        }

        SpaceComment spaceComment = SpaceComment.builder()
                .postId(publishSpaceCommentDTO.getPostId())
                .publishId(publishSpaceCommentDTO.getPublishId())
                .content(publishSpaceCommentDTO.getContent())
                .status(SpaceConstants.NORMAL_STATUS)
                .createdTime(LocalDateTime.now())
                .build();
        spaceCommentMapper.insert(spaceComment);

        // 增加帖子评论数
        spacePostMapper.updateAddCommentsCountById(spacePost);
        return Result.success("评论发布成功");
    }

    @Override
    public Result<String> delete(Long commentId) {
        SpaceComment spaceComment = spaceCommentMapper.selectById(commentId);
        if(spaceComment == null || SpaceConstants.DISABLE_STATUS.equals(spaceComment.getStatus())){
            return Result.error("评论不存在或已被删除");
        }
        if(!spaceComment.getPublishId().equals(UserContext.getCurUserId())){
            return Result.error("非评论发布人，不能删除");
        }
        spaceComment.setStatus(SpaceConstants.DISABLE_STATUS);
        spaceCommentMapper.updateDisableStatusById(spaceComment);

        // 减少帖子评论数
        SpacePost spacePost = spacePostMapper.selectById(spaceComment.getPostId());
        spacePostMapper.updateSubCommentsCountById(spacePost);
        return Result.success("评论删除成功");
    }

    @Override
    public Result<List<SpaceCommentVO>> list(Long postId) {
        List<SpaceCommentVO> commentList = spaceCommentMapper.selectListByPostId(postId);
        return Result.success(commentList);
    }
}
