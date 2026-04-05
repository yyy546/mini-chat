package com.minichat.space.service;

import com.minichat.common.result.Result;
import com.minichat.space.dto.PublishSpacePostDTO;
import com.minichat.space.vo.SpacePostVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SpacePostService {

    /**
     * 发布帖子
     */
    Result<String> publish(PublishSpacePostDTO publishSpacePostDTO);

    /**
     * 上传图片
     */
    Result<String> uploadImage(MultipartFile file);

    /**
     * 获取帖子列表
     */
    Result<List<SpacePostVO>> list(Long userId, Long friendId);

    /**
     * 删除帖子
     */
    Result<String> delete(Long postId);

    /**
     * 恢复帖子
     */
    Result<String> recover(Long postId);

    /**
     * 获取用户删除的帖子列表
     */
    Result<List<SpacePostVO>> deletedList(Long userId);

     /**
      * 切换帖子点赞状态
      */
    Result<String> changeLikeStatus(Long postId);
}
