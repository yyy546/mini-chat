package com.minichat.chat.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.minichat.chat.vo.GroupMessageVO;
import com.minichat.chat.entity.GroupMessage;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface GroupMessageMapper {

    @Insert("insert into group_message (message_seq, group_id, sender_id, message_type, content, file_url, file_name, file_size, is_recall, send_time) " +
            "values (#{messageSeq}, #{groupId}, #{senderId}, #{messageType}, #{content}, #{fileUrl}, #{fileName}, #{fileSize}, #{isRecall}, #{sendTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(GroupMessage groupMessage);

    @Select("select " +
            "gm.id as message_id, " +
            "gm.message_seq as message_seq, " +
            "gm.group_id as group_id, " +
            "gm.sender_id as sender_id, " +
            "u.nickname as sender_nickname, " +
            "u.avatar as sender_avatar, " +
            "gm.message_type as message_type, " +
            "gm.content as content, " +
            "gm.file_url as file_url, " +
            "gm.file_name as file_name, " +
            "gm.file_size as file_size, " +
            "gm.is_recall as is_recall, " +
            "gm.send_time as send_time " +
            "from group_message gm " +
            "left join user u on gm.sender_id = u.id " +
            "where gm.group_id = #{groupId} " +
            "order by send_time desc")
    IPage<GroupMessageVO> selectByGroupId(IPage<GroupMessageVO> page, @Param("groupId") Long groupId);

    @Select("select * from group_message where group_id = #{groupId} order by send_time desc limit 1")
    GroupMessage selectLastMessageByGroupId(Long groupId);

    @Select("select count(*) from group_message where group_id = #{groupId} and message_seq > #{lastReadMessageId}")
    int countUnreadMessages(@Param("groupId") Long groupId, @Param("lastReadMessageId") Long lastReadMessageId);

    @Delete("delete from group_message where group_id = #{groupId}")
    void deleteByGroupId(Long groupId);

    @Select("select * from group_message where id = #{id}")
    GroupMessage selectById(Long id);

    @Update("update group_message set is_recall = #{isRecall}, recall_time = #{recallTime} where id = #{id}")
    void updateIsRecall(GroupMessage groupMessage);

    @Select("select id from group_message where is_recall = 1 and recall_time < #{localDateTime}")
    List<Long> selectExpiredRecallMessageIds(LocalDateTime localDateTime);

    void deleteBatch(List<Long> recallGroupMessageIds);

    List<GroupMessage> selectLastMessageBatch(@Param("groupIds") List<Long> groupIds);
}

