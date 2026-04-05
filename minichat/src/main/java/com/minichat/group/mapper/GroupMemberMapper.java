package com.minichat.group.mapper;

import com.minichat.group.dto.GroupMemberLastReadDTO;
import com.minichat.group.vo.GroupMemberVO;
import com.minichat.group.vo.GroupVO;
import com.minichat.group.entity.GroupMember;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface GroupMemberMapper {

    @Insert("insert into group_member(group_id, user_id, role, is_muted, is_deleted, nickname_in_group, join_time) values" +
            "(#{groupId}, #{userId}, #{role}, #{isMuted}, #{isDeleted}, #{nicknameInGroup}, #{joinTime})")
    void insert(GroupMember groupMember);

    List<GroupVO> selectGroupVOByUserId(Long currentUserId);

    @Select("select count(*) from group_member where group_id = #{groupId} and user_id = #{userId} and is_muted = 0")
    int selectByGroupIdAndUserId(@Param("groupId") @NotNull(message = "群聊ID不能为空") @Positive(message = "群聊ID必须为正整数") Long groupId, @Param("userId") Long realSenderId);

    @Select("select last_read_message_id from group_member where group_id = #{groupId} and user_id = #{userId}")
    Long selectLastReadMessageId(@Param("groupId") Long groupId, @Param("userId") Long currentUserId);

    @Update("update group_member set last_read_message_id = #{messageSeq} where group_id = #{groupId} and user_id = #{currentUserId}")
    void updateLastReadMessageId(@Param("groupId") Long groupId, @Param("currentUserId") Long currentUserId, @Param("messageSeq") Long messageSeq);

    @Select("select role from group_member where group_id = #{groupId} and user_id = #{currentUserId}")
    Integer selectGroupMemberRoleByGroupIdAndUserId(Long groupId, Long currentUserId);

    @Select("select user_id from group_member where group_id = #{groupId} and role = 1")
    List<Long> selectAdminIdsByGroupId(Long groupId);

    @Select("select group_member.user_id, u.avatar, u.nickname, nickname_in_group, role, is_muted, join_time from group_member " +
            "left join user u on group_member.user_id = u.id where group_id = #{groupId}")
    List<GroupMemberVO> selectGroupMemberVOByGroupId(Long groupId);

    @Select("select user_id from group_member where group_id = #{groupId}")
    List<Long> selectMemberIdsByGroupId(Long groupId);

    @Delete("delete from group_member where group_id = #{groupId} and user_id = #{userId}")
    void deleteByGroupIdAndUserId(@NotNull(message = "群组ID不能为空") Long groupId, @NotNull(message = "群组成员ID不能为空") Long userId);

    @Update("update group_member set role = #{role} where group_id = #{groupId} and user_id = #{userId}")
    void updateRole(@NotNull(message = "群组ID不能为空") Long groupId, @NotNull(message = "用户ID不能为空") Long userId, @NotNull(message = "角色不能为空") Integer role);

    @Delete("delete from group_member where group_id = #{groupId}")
    void deleteByGroupId(Long groupId);

    List<GroupMemberLastReadDTO> selectLastReadBatch(@Param("currentUserId") Long currentUserId, @Param("groupIds") List<Long> groupIds);
}
