package com.minichat.group.mapper;

import com.minichat.group.vo.ReceivedGroupRequestVO;
import com.minichat.group.vo.SentGroupRequestVO;
import com.minichat.group.entity.GroupRequest;
import jakarta.validation.constraints.NotNull;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GroupRequestMapper {
    @Select("select * from group_request where group_id = #{groupId} and applicant_id = #{applicantId}")
    GroupRequest selectByGroupIdAndUserId(@NotNull(message = "群ID不能为空") Long groupId, @NotNull(message = "申请人ID不能为空") Long applicantId);

    void update(GroupRequest groupRequest);

    @Insert("insert into group_request(group_id, applicant_id, message, status, created_time) " +
            "values(#{groupId}, #{applicantId}, #{message}, #{status}, #{createdTime})")
    void insert(GroupRequest newGroupRequest);

    @Select("select gr.group_id as groupId, gr.applicant_id as applicantId, gr.message as message, gr.status as status, gr.created_time as createdTime, " +
            "g.group_name as groupName, g.avatar as avatar " +
            "from group_request gr " +
            "left join chat_group g on gr.group_id = g.id " +
            "where gr.applicant_id = #{currentUserId}")
    List<SentGroupRequestVO> selectSentGroupRequests(Long currentUserId);

    @Select("select gr.id as requestId, gr.group_id as groupId, g.group_name as groupName, " +
            "gr.applicant_id as applicantId, u.nickname as applicantName, u.avatar as applicantAvatar, " +
            "gr.message as message, gr.created_time as createdTime, gr.status as status " +
            "from group_request gr " +
            "join group_member gm on gr.group_id = gm.group_id " +
            "join user u on gr.applicant_id = u.id " +
            "join chat_group g on gr.group_id = g.id " +
            "where gm.user_id = #{currentUserId} " +
            "and (gm.role = 1 or gm.role = 2) " +
            "order by gr.created_time desc")
    List<ReceivedGroupRequestVO> selectReceivedGroupRequests(Long currentUserId);
}
