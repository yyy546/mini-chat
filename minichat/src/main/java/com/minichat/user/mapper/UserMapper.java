package com.minichat.user.mapper;

import com.minichat.user.vo.UserSearchVO;
import com.minichat.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Set;

@Mapper
public interface UserMapper {
    @Select("select 1 from user limit 1")
    Integer warmUp();

    @Select("select * from user where username = #{username}")
    User selectByUsername(@NotBlank(message = "用户名不能为空") @Size(min = 3, max = 20, message = "用户名长度必须在3到20之间") String username);

    @Insert("insert into user (username, password, nickname, avatar, gender, signature, status, created_time, updated_time) " +
            "values (#{username}, #{password}, #{nickname}, #{avatar}, #{gender}, #{signature}, #{status}, now(), now())")
    void insert(User user);

    @Update("update user set last_login_time = now() where id = #{id}")
    void updateLastLoginTime(Long id);

    List<UserSearchVO> searchUsers(String keyword, Long currentUserId);

    @Select("select * from user where id = #{id}")
    User selectById(Long id);

    @Select("select nickname from user where id = #{userId}")
    String getNickNameById(Long userId);

    void updateById(User user);

    @Update("update user set avatar = #{avatarUrl}, updated_time = now() where id = #{currentUserId}")
    void updateAvatar(@Param("currentUserId") Long currentUserId, @Param("avatarUrl") String avatarUrl);

    List<User> selectBatchIds(Set<Long> userIds);
}
