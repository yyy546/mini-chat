package com.minichat.space.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.minichat.common.constants.MqConstants;
import com.minichat.common.constants.SpaceConstants;
import com.minichat.common.result.Result;
import com.minichat.common.util.UserContext;
import com.minichat.friend.mapper.FriendMapper;
import com.minichat.space.dto.PublishSpacePostDTO;
import com.minichat.space.dto.SpacePostMqDTO;
import com.minichat.space.entity.SpaceLike;
import com.minichat.space.entity.SpacePost;
import com.minichat.space.mapper.SpaceLikeMapper;
import com.minichat.space.mapper.SpacePostMapper;
import com.minichat.space.service.SpacePostService;
import com.minichat.space.vo.SpacePostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.minichat.common.util.OssFileUtil;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SpacePostServiceImpl implements SpacePostService {

    private final SpacePostMapper spacePostMapper;
    private final SpaceLikeMapper spaceLikeMapper;
    private final OssFileUtil ossFileUtil;
    private final FriendMapper friendMapper;
    private final RabbitTemplate rabbitTemplate;

    @Value("${aliyun.oss.space-post-path}")
    private String spacePostPath;

    @Override
    public Result<String> uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }
        try {
            // 校验是否为图片
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            }
            if (!OssFileUtil.isImageFile(extension, file.getContentType())) {
                return Result.error("只能上传图片文件");
            }

            String url = ossFileUtil.uploadFile(file, spacePostPath);
            return Result.success("上传成功", url);
        } catch (IOException e) {
            log.error("空间图片上传失败", e);
            return Result.error("图片上传失败");
        }
    }

    @Override
    public Result<List<SpacePostVO>> list(Long userId, Long friendId) {
        Long currentUserId = UserContext.getCurUserId();
        if(!currentUserId.equals(userId)){
            return Result.error("用户ID错误");
        }
        if(!friendMapper.isFriend(userId, friendId)){
            return Result.error("不是好友关系");
        }
        List<SpacePostVO> spacePostVOList = spacePostMapper.selectListByAuthorId(friendId, currentUserId);

        return Result.success(spacePostVOList);
    }

    @Override
    public Result<String> delete(Long postId) {
        SpacePost spacePost = validatePostOwnership(postId);
        spacePostMapper.updateStatusById(postId, SpaceConstants.DISABLE_STATUS);
        // 发送删除消息到MQ
        SpacePostMqDTO spacePostMqDTO = SpacePostMqDTO.builder()
                .authorId(UserContext.getCurUserId())
                .spacePostId(postId)
                .timestamp(spacePost.getCreatedTime().toInstant(ZoneOffset.of("+8")).toEpochMilli())
                .build();
        rabbitTemplate.convertAndSend(MqConstants.SPACE_POST_EXCHANGE,
                MqConstants.SPACE_POST_DELETE_ROUTING_KEY,
                spacePostMqDTO);
        return Result.success("删除成功");
    }

    @Override
    public Result<String> recover(Long postId) {
        SpacePost spacePost = validatePostOwnership(postId);
        spacePostMapper.updateStatusById(postId, SpaceConstants.NORMAL_STATUS);
        // 发送恢复消息到MQ
        SpacePostMqDTO spacePostMqDTO = SpacePostMqDTO.builder()
                .authorId(UserContext.getCurUserId())
                .spacePostId(postId)
                .timestamp(spacePost.getCreatedTime().toInstant(ZoneOffset.of("+8")).toEpochMilli())
                .build();
        rabbitTemplate.convertAndSend(MqConstants.SPACE_POST_EXCHANGE,
                MqConstants.SPACE_POST_RECOVER_ROUTING_KEY,
                spacePostMqDTO);
        return Result.success("恢复成功");
    }

    @Override
    public Result<List<SpacePostVO>> deletedList(Long userId) {
        Long currentUserId = UserContext.getCurUserId();
        if(!currentUserId.equals(userId)){
            return Result.error("用户ID错误");
        }
        List<SpacePostVO> spacePostVOList = spacePostMapper.selectListByUserIdWithDisabledStatus(userId, SpaceConstants.DISABLE_STATUS, currentUserId);

        return Result.success(spacePostVOList);
    }

    private SpacePost validatePostOwnership(Long postId){
        Long userId = UserContext.getCurUserId();
        SpacePost spacePost = spacePostMapper.selectById(postId);
        if(spacePost == null){
            throw new IllegalArgumentException("帖子不存在");
        }
        if(!spacePost.getAuthorId().equals(userId)){
            throw new IllegalArgumentException("用户ID错误");
        }
        return spacePost;
    }

    @Override
    @Transactional
    public Result<String> changeLikeStatus(Long postId) {
        Long curUserId = UserContext.getCurUserId();
        if(curUserId == null){
            return Result.error("用户未登录");
        }
        SpacePost spacePost = spacePostMapper.selectById(postId);
        if(spacePost == null || SpaceConstants.DISABLE_STATUS.equals(spacePost.getStatus())){
            return Result.error("帖子不存在或已被删除");
        }
        int deletedRows = spaceLikeMapper.deleteByPostIdAndUserId(postId, curUserId);
        if(deletedRows > 0){
            spacePostMapper.decrementLikesCount(postId, LocalDateTime.now());
            return Result.success("取消点赞成功");
        }

        SpaceLike newSpaceLike = SpaceLike.builder()
                .postId(postId)
                .userId(curUserId)
                .createdTime(LocalDateTime.now())
                .build();
        int insertedRows = spaceLikeMapper.insertIgnore(newSpaceLike);
        if(insertedRows > 0){
            spacePostMapper.incrementLikesCount(postId, LocalDateTime.now());
        }
        return Result.success("点赞成功");
    }

    @Override
    public Result<String> publish(PublishSpacePostDTO publishSpacePostDTO) {
        if(publishSpacePostDTO.getContent() == null && (publishSpacePostDTO.getImages() == null || publishSpacePostDTO.getImages().isEmpty())){
            return Result.error("发布内容不能为空");
        }

        JSONArray imagesArray = null;
        if (publishSpacePostDTO.getImages() != null) {
            imagesArray = new JSONArray();
            imagesArray.addAll(publishSpacePostDTO.getImages());
        }

        SpacePost spacePost = SpacePost.builder()
                .authorId(publishSpacePostDTO.getAuthorId())
                .content(publishSpacePostDTO.getContent())
                .images(imagesArray)
                .imagesCount(imagesArray == null ? 0 : imagesArray.size())
                .commentsCount(SpaceConstants.INIT_SPACE_COMMENTS_COUNT)
                .likesCount(SpaceConstants.INIT_SPACE_LIKES_COUNT)
                .status(SpaceConstants.NORMAL_STATUS)
                .createdTime(LocalDateTime.now())
                .updatedTime(LocalDateTime.now())
                .build();

        spacePostMapper.insert(spacePost);
        //
        SpacePostMqDTO spacePostMqDTO = SpacePostMqDTO.builder()
                .spacePostId(spacePost.getId())
                .authorId(spacePost.getAuthorId())
                .timestamp(spacePost.getCreatedTime().toInstant(ZoneOffset.of("+8")).toEpochMilli())
                .build();
        //异步推送帖子ID
        rabbitTemplate.convertAndSend(MqConstants.SPACE_POST_EXCHANGE,
                MqConstants.SPACE_POST_ROUTING_KEY, spacePostMqDTO);
        return Result.success("发布成功");
    }
}
