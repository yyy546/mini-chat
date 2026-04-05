package com.minichat.space.mapper;

import com.minichat.space.entity.SpaceComment;
import com.minichat.space.vo.SpaceCommentVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SpaceCommentMapper {

    @Select("select * from space_comment where post_id = #{id} and status = 0 order by created_time desc limit 3")
    List<SpaceCommentVO> selectLimitListByPostId(Long id);

    List<SpaceCommentVO> selectListByPostId(Long id);

    @Insert("insert into space_comment(post_id, publish_id, content, status, created_time) " +
            "values(#{postId}, #{publishId}, #{content}, #{status}, #{createdTime})")
    void insert(SpaceComment spaceComment);

    @Select("select * from space_comment where id = #{commentId}")
    SpaceComment selectById(Long commentId);

    @Update("update space_comment set status = #{status} where id = #{id}")
    void updateDisableStatusById(SpaceComment spaceComment);

    List<SpaceCommentVO> selectLimitListByPostIds(@Param("postIds") List<Long> postIds);

    void deleteBatchByPostIds(List<Long> expirePostIds);
}
