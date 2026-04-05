package com.minichat.friend.mapper;

import com.minichat.friend.vo.FriendRequestVO;
import com.minichat.friend.vo.SentFriendRequestVO;
import com.minichat.friend.entity.FriendRequest;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FriendRequestMapper {
    @Insert("insert ignore into friend_request(from_user_id, to_user_id, message, status, created_time) " +
            "values(#{fromUserId}, #{toUserId}, #{message}, #{status}, now())")
    void insert(FriendRequest friendRequest);

    @Select("select * from friend_request where from_user_id = #{curUserId} and to_user_id = #{toUserId}")
    FriendRequest selectByFromUserIdAndToUserId(Long curUserId, Long toUserId);

    @Select("select * from friend_request where id = #{requestId}")
    FriendRequest selectById(Long requestId);

    void update(FriendRequest friendRequest);

    List<FriendRequestVO> selectReceivedRequests(Long currentUserId);

    @Delete("delete from friend_request where id = #{requestId}")
    void delete(Long requestId);

    List<SentFriendRequestVO> selectSentRequests(Long currentUserId);
}
