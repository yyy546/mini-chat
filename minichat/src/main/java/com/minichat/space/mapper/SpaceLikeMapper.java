package com.minichat.space.mapper;

import com.minichat.space.entity.SpaceLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SpaceLikeMapper{

    @Insert("insert ignore into space_like (post_id, user_id, created_time) values (#{postId}, #{userId}, #{createdTime})")
    int insertIgnore(SpaceLike newSpaceLike);

    @Select("select * from space_like where post_id = #{postId} and user_id = #{curUserId}")
    SpaceLike selectOne(@Param("postId") Long postId, @Param("curUserId") Long curUserId);

    @Delete("delete from space_like where post_id = #{postId} and user_id = #{userId}")
    int deleteByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    void deleteBatchByPostIds(List<Long> expirePostIds);
}
