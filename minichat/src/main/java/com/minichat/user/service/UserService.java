package com.minichat.user.service;

import com.minichat.user.dto.UserUpdateDTO;
import com.minichat.user.vo.UserDetailVO;
import com.minichat.user.vo.UserLoginVO;
import com.minichat.user.vo.UserSearchVO;
import com.minichat.user.dto.LoginDTO;
import com.minichat.user.dto.RegisterDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {

    /**
     * 注册用户
     */
    void register(RegisterDTO registerDTO);

    /**
     * 登录用户
     */
    UserLoginVO login(LoginDTO loginDTO);

    /**
     * 获取当前登录用户的信息
     */
    UserLoginVO me(String username);

    /**
     * 搜索用户
     */
    List<UserSearchVO> searchUsers(String keyword, Long currentUserId);

    /**
     * 根据用户ID获取用户登录信息
     */
    UserLoginVO getUserLoginVO(Long userId);

    /**
     * 根据用户ID获取用户昵称
     */
    String getNicknameById(Long userId);

    /**
     * 根据用户ID获取用户详细信息
     */
    UserDetailVO getUserDetail(Long currentUserId);

    /**
     * 更新用户详细信息
     */
    UserDetailVO updateUserDetail(Long currentUserId, UserUpdateDTO userUpdateDTO);

    /**
     * 上传用户头像
     */
    String uploadAvatar(Long currentUserId, MultipartFile avatar);
}
