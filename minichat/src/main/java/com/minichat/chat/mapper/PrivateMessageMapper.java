package com.minichat.chat.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.minichat.chat.dto.MessageUnreadCountDTO;
import com.minichat.chat.entity.PrivateMessage;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface PrivateMessageMapper {
    @Select("select * from private_message where ((sender_id = #{currentUserId} and receiver_id = #{targetUserId}) or (sender_id = #{targetUserId}" +
            " and receiver_id = #{currentUserId})) order by send_time desc")
    IPage<PrivateMessage> selectHistoryByTwoUsers(IPage<PrivateMessage> page, @Param("currentUserId") Long currentUserId, @Param("targetUserId") Long targetUserId);

    void insert(PrivateMessage privateMessage);

    void updateIsRead(Long currentUserId, Long receiverId);

    @Select("select * from private_message where (sender_id = #{currentUserId} and receiver_id = #{friendId}) or (sender_id = #{friendId} " +
            "and receiver_id = #{currentUserId}) and is_recall = 0 order by send_time desc limit 1")
    PrivateMessage selectLastMessageByTwoUsers(Long currentUserId, Long friendId);

    @Select("select count(*) from private_message where receiver_id = #{currentUserId} and sender_id = #{friendId} and is_read = 0 and is_recall = 0")
    Long selectUnreadCount(Long currentUserId, Long friendId);


    @Select("select * from private_message where id = #{messageId}")
    PrivateMessage selectById(Long messageId);

    @Update("update private_message set is_recall = #{isRecall}, recall_time = #{recallTime} where id = #{id}")
    void updateIsRecall(PrivateMessage privateMessage);

    @Select("select id from private_message where is_recall = 1 and recall_time < #{localDateTime}")
    List<Long> selectExpiredRecallMessageIds(LocalDateTime localDateTime);

    void deleteBatch(List<Long> recallPrivateMessageIds);

    // 批量查询未读数，返回 Map 的简易封装对象或 List<Map>
    List<MessageUnreadCountDTO> selectUnreadCountBatch( @Param("currentUserId") Long currentUserId, @Param("friendIds") List<Long> friendIds);

    List<PrivateMessage> selectLastMessageBatch(@Param("currentUserId") Long currentUserId, @Param("friendIds") List<Long> friendIds);
}
