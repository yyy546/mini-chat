package com.minichat.group.mapper;

import com.minichat.group.vo.GroupSearchVO;
import com.minichat.group.vo.GroupVO;
import com.minichat.group.entity.ChatGroup;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatGroupMapper {

    @Insert("INSERT INTO chat_group (group_name, avatar, announcement, creator_id, owner_id, member_count, max_members, join_policy, invite_policy, is_deleted, created_time, updated_time) " +
            "VALUES (#{groupName}, #{avatar}, #{announcement}, #{creatorId}, #{ownerId}, #{memberCount}, #{maxMembers}, #{joinPolicy}, #{invitePolicy}, #{isDeleted}, #{createdTime}, #{updatedTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ChatGroup group);

    @Select("SELECT * FROM chat_group WHERE id = #{groupId} and is_deleted = 0")
    ChatGroup selectById(@NotNull(message = "群聊ID不能为空") @Positive(message = "群聊ID必须为正整数") Long groupId);

    @Select("SELECT id, group_name AS groupName, avatar, announcement, creator_id AS creatorId, owner_id AS ownerId, member_count AS memberCount, max_members AS maxMembers, " +
            "join_policy AS joinPolicy, invite_policy AS invitePolicy, created_time AS createdTime FROM chat_group WHERE id = #{groupId} and is_deleted = 0")
    GroupVO selectGroupVOById(Long groupId);

    void update(ChatGroup group);

    @Update("UPDATE chat_group SET avatar = #{newAvatarUrl}, updated_time = now() WHERE id = #{groupId}")
    void updateAvatar(Long groupId, String newAvatarUrl);

    @Update("update chat_group set member_count = #{memberCount}, updated_time = now() where id = #{groupId}")
    void updateMemberCount(Long groupId, int memberCount);

    @Delete("delete from chat_group where id = #{groupId}")
    void deleteById(Long groupId);

    @Update("update chat_group set owner_id = #{newOwnerId}, updated_time = now() where id = #{groupId}")
    void updateOwner(Long groupId, Long newOwnerId);

    List<GroupSearchVO> searchGroups(String keyword, Long currentUserId);
}
