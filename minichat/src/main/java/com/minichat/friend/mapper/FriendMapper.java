package com.minichat.friend.mapper;

import com.minichat.friend.dto.FriendGroupUpdateDTO;
import com.minichat.friend.dto.FriendRemarkUpdateDTO;
import com.minichat.friend.vo.FriendDetailVO;
import com.minichat.friend.vo.FriendGroupItemVO;
import com.minichat.friend.vo.FriendGroupVO;
import com.minichat.friend.vo.FriendVO;
import com.minichat.friend.entity.Friend;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FriendMapper {

    List<FriendVO> selectFriendList(Long currentUserId);

    @Insert("insert ignore into friend(user_id, friend_id, remark_name, group_name, is_blocked, is_deleted, created_time) " +
            "values(#{userId}, #{friendId}, #{remarkName}, #{groupName}, #{isBlocked}, #{isDeleted}, #{createdTime})")
    void insert(Friend friend1);

    @Select("select count(*) from friend where user_id = #{curUserId} and friend_id = #{toUserId} and is_deleted = 0")
    int selectFriendByUserIdAndFriendId(Long curUserId, Long toUserId);

    @Update("update friend set remark_name = #{friendRemarkUpdateDTO.remarkName} " +
            "where user_id = #{currentUserId} and friend_id = #{friendRemarkUpdateDTO.friendId}")
    void updateFriendRemark(@Param("currentUserId") Long currentUserId, @Param("friendRemarkUpdateDTO") FriendRemarkUpdateDTO friendRemarkUpdateDTO);

    @Select("    select group_name as groupName, count(*) as friendCount, sum(is_blocked) as totalCount \n" +
            "    from friend \n" +
            "    where user_id = #{currentUserId} \n" +
            "    group by group_name")
    List<FriendGroupVO> selectFriendGroupList(Long currentUserId);

    List<FriendGroupItemVO> selectFriendGroupItemList(Long currentUserId, String groupName);

    FriendDetailVO selectFriendDetail(Long currentUserId, Long friendId);

    // 检查是否存在好友记录（包括已删除的）
    @Select("select * from friend where user_id = #{userId} and friend_id = #{friendId}")
    Friend selectFriendRecord(@Param("userId") Long userId, @Param("friendId") Long friendId);

    // 恢复已删除的好友关系
    @Update("update friend set is_deleted = 0, deleted_time = null, " +
            "group_name = #{groupName}, is_blocked = #{isBlocked} " +
            "where user_id = #{userId} and friend_id = #{friendId}")
    void restoreFriend(Friend friend);

    // 软删除好友关系
    @Update("update friend set is_deleted = 1, deleted_time = #{deletedTime} " +
            "where user_id = #{userId} and friend_id = #{friendId} and is_deleted = 0")
    void deleteFriend(@Param("userId") Long userId, @Param("friendId") Long friendId, @Param("deletedTime") LocalDateTime deletedTime);

    @Update("update friend set group_name = #{friendGroupUpdateDTO.groupName} " +
            "where user_id = #{currentUserId} and friend_id = #{friendGroupUpdateDTO.friendId} and is_deleted = 0")
    void updateFriendGroup(@Param("currentUserId") Long currentUserId, @Param("friendGroupUpdateDTO") FriendGroupUpdateDTO friendGroupUpdateDTO);

    @Select("select count(*) from friend where user_id = #{userId} and friend_id = #{friendId} " +
            "or user_id = #{friendId} and friend_id = #{userId} and is_deleted = 0")
    boolean isFriend(Long userId, Long friendId);

    @Select("select user_id from friend where friend_id = #{friendId} and is_deleted = 0")
    List<Long> selectUserIdsByFriendId(Long friendId);
}
