package com.minichat.space.mapper;

import com.minichat.space.entity.SpacePost;
import com.minichat.space.vo.SpacePostVO;
import jakarta.validation.constraints.NotNull;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SpacePostMapper{

    @Select("select * from space_post where id = #{postId}")
    SpacePost selectById(Long postId);
    List<SpacePostVO> selectListByUserIdAndFriendIds(@Param("userId") Long userId, @Param("friendIds") List<Long> friendIds, @Param("currentUserId") Long currentUserId);

    List<SpacePostVO> selectListByUserIdWithDisabledStatus(@Param("userId") Long userId, @Param("disableStatus") Integer disableStatus, @Param("currentUserId") Long currentUserId);

    @Update("update space_post set status = #{status}, updated_time = now() where id = #{postId}")
    void updateStatusById(@Param("postId") Long postId, @Param("status") Integer status);

    @Update("update space_post set likes_count = likes_count + 1, updated_time = #{updatedTime} where id = #{postId}")
    int incrementLikesCount(@Param("postId") Long postId, @Param("updatedTime") LocalDateTime updatedTime);

    @Update("update space_post set likes_count = case when likes_count > 0 then likes_count - 1 else 0 end, updated_time = #{updatedTime} where id = #{postId}")
    int decrementLikesCount(@Param("postId") Long postId, @Param("updatedTime") LocalDateTime updatedTime);

    @Insert("insert into space_post (author_id, content, images, images_count, comments_count, likes_count, status, created_time, updated_time) values" +
            " (#{authorId}, #{content}, #{images, typeHandler=com.baomidou.mybatisplus.extension.handlers.Fastjson2TypeHandler}" +
            ", #{imagesCount}, #{commentsCount}, #{likesCount}, #{status}, #{createdTime}, #{updatedTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SpacePost spacePost);

    @Update("update space_post set comments_count = comments_count + 1, updated_time = now() where id = #{id}")
    void updateAddCommentsCountById(SpacePost spacePost);

    @Update("update space_post set comments_count = case when comments_count > 0 then comments_count - 1 else 0 end, updated_time = now() where id = #{id}")
    void updateSubCommentsCountById(SpacePost spacePost);

    @Select("select id from space_post where status = 1 and updated_time < #{expireTime} limit #{limit}")
    List<Long> selectExpirePostIds(@Param("expireTime") LocalDateTime expireTime, @Param("limit") int limit);

    void deleteBatchByIds(@Param("expirePostIds") List<Long> expirePostIds);

    List<SpacePostVO> selectListByIds(List<Long> ids, Long currentUserId);

    @Select("select id from space_post where author_id = #{authorId}")
    List<Long> selectPostIdsByAuthorId(Long authorId);

    List<SpacePostVO> selectListByAuthorId(@Param("authorId") @NotNull(message = "作者ID不能为空") Long authorId, @Param("currentUserId") Long currentUserId);
}
