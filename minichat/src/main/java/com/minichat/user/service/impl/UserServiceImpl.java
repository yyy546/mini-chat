package com.minichat.user.service.impl;

import com.alibaba.fastjson2.TypeReference;
import com.minichat.common.constants.RedisConstants;
import com.minichat.common.util.CacheClient;
import com.minichat.user.dto.UserUpdateDTO;
import com.minichat.user.service.LoginAsyncService;
import com.minichat.user.vo.UserDetailVO;
import com.minichat.user.vo.UserLoginVO;
import com.minichat.user.vo.UserSearchVO;
import com.minichat.user.dto.LoginDTO;
import com.minichat.user.dto.RegisterDTO;
import com.minichat.common.constants.OssConstants;
import com.minichat.common.constants.UserConstants;
import com.minichat.user.entity.User;
import com.minichat.common.enums.GenderEnum;
import com.minichat.user.mapper.UserMapper;
import com.minichat.friend.mapper.FriendMapper;
import com.minichat.user.service.UserService;
import com.minichat.common.util.JwtUtil;
import com.minichat.common.util.OssFileUtil;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;      // 密码加密器
    private final JwtUtil jwtUtil;
    private final OssFileUtil ossFileUtil;
    private final CacheClient cacheClient;
    private final RedissonClient redissonClient;
    private final LoginAsyncService loginAsyncService;
    private final FriendMapper friendMapper;

    // 从配置文件读取OSS头像存储路径
    @Value("${aliyun.oss.avatar-path:avatar/}")
    private String ossAvatarPath;

    @Override
    @Transactional
    public void register(RegisterDTO registerDTO) {
        String username = registerDTO.getUsername();
        RLock lock = redissonClient.getLock(RedisConstants.LOCK_REGISTER_KEY_PREFIX + username);
        try {
            if(!lock.tryLock(5, TimeUnit.SECONDS)) {
                throw new IllegalArgumentException("注册过于频繁，请稍后重试");
            }

            // 检查用户名是否已存在
            User existingUser = userMapper.selectByUsername(username);
            if(existingUser != null) {
                throw new IllegalArgumentException("用户名已存在");
            }
            //创建新用户
            User user = User.builder()
                    .username(registerDTO.getUsername())
                    .password(passwordEncoder.encode(registerDTO.getPassword()))
                    .nickname(registerDTO.getNickname())
                    .avatar(OssConstants.DEFAULT_AVATAR_URL)
                    .gender(GenderEnum.UNKNOWN.getCode())
                    .signature(UserConstants.DEFAULT_SIGNATURE)
                    .status(UserConstants.NORMAL_STATUS)
                    .build();

            userMapper.insert(user);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("注册失败，请稍后重试");
        }catch (DuplicateKeyException e){
            log.warn("用户名重复注册，用户名：{}", registerDTO.getUsername(),e);
            throw new IllegalArgumentException("用户名已存在");
        }finally {
            if(lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }

    @Override
    public UserLoginVO login(LoginDTO loginDTO) {
        User user = userMapper.selectByUsername(loginDTO.getUsername());
        // 1.检查用户名是否存在
        if(Objects.isNull(user)) {
            throw new IllegalArgumentException("用户名不存在");
        }
        // 2.检查密码是否匹配
        if(!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }
        // 3.检查用户状态
        if(UserConstants.DISABLE_STATUS.equals(user.getStatus())) {
            throw new IllegalArgumentException("用户已被禁用");
        }
        Long userId = user.getId();
        // 4.异步更新用户最后登录时间
        loginAsyncService.asyncUpdateLastLoginTime(userId);
        // 5.生成JWT token
        String token = jwtUtil.generateToken(userId);
        // 6.返回登录成功VO
        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(userId)
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .gender(user.getGender())
                .signature(user.getSignature())
                .token(token)
                .build();
        return userLoginVO;
    }

    @Override
    public UserLoginVO me(String username) {
        User user = userMapper.selectByUsername(username);
        // 检查用户是否存在
        if(Objects.isNull(user)){
            throw new IllegalArgumentException("用户不存在或已被删除");
        }
        UserLoginVO userLoginVO = new UserLoginVO();
        BeanUtils.copyProperties(user, userLoginVO);

        return userLoginVO;
    }

    @Override
    public List<UserSearchVO> searchUsers(String keyword, Long currentUserId) {
        List<UserSearchVO> userSearchVOList = userMapper.searchUsers(keyword, currentUserId);
        return userSearchVOList;
    }

    @Override
    public UserLoginVO getUserLoginVO(Long userId) {
        User user = userMapper.selectById(userId);
        if(Objects.isNull(user)){
            throw new IllegalArgumentException("用户不存在或已被删除");
        }
        UserLoginVO userLoginVO = new UserLoginVO();
        BeanUtils.copyProperties(user, userLoginVO);
        return userLoginVO;
    }

    @Override
    public String getNicknameById(Long userId) {
        return userMapper.getNickNameById(userId);
    }

    @Override
    public UserDetailVO getUserDetail(Long currentUserId) {
        if(currentUserId == null){
            throw new IllegalArgumentException("用户ID不能为空");
        }

        UserDetailVO userDetailVO = cacheClient.queryWithPassThrough(RedisConstants.CACHE_USER_DETAIL_KEY, currentUserId, new TypeReference<UserDetailVO>() {},
                userId -> {
                    User user = userMapper.selectById(userId);
                    String gender = GenderEnum.getTextByCode(user.getGender());
                    return UserDetailVO.builder()
                            .id(user.getId())
                            .nickname(user.getNickname())
                            .avatar(user.getAvatar())
                            .gender(gender)
                            .signature(user.getSignature())
                            .createdTime(user.getCreatedTime())
                            .build();
                },
                RedisConstants.CACHE_NORMAL_EXPIRE_TIME + new Random().nextLong(10), TimeUnit.MINUTES);

        return userDetailVO;
    }

    @Override
    public String uploadAvatar(Long currentUserId, MultipartFile avatar) {
        // 1. 基础校验
        if (avatar == null || avatar.isEmpty()) {
            throw new IllegalArgumentException("头像文件不能为空");
        }
        // 2. 查询用户（确保用户存在）
        User user = userMapper.selectById(currentUserId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在或已被删除");
        }

        String newAvatarUrl = null;
        try {
            // ========== 关键修改：增加默认头像判断，仅删除非默认的旧头像 ==========
            String oldAvatarUrl = user.getAvatar();
            if (StringUtils.isNotBlank(oldAvatarUrl)) {
                // 用Objects.equals避免空指针，且匹配更严谨
                if (!Objects.equals(oldAvatarUrl, OssConstants.DEFAULT_AVATAR_URL)) {
                    ossFileUtil.deleteFile(oldAvatarUrl);
                    log.info("用户{}旧头像（非默认）已从OSS删除，URL：{}", currentUserId, oldAvatarUrl);
                } else {
                    log.info("用户{}旧头像为系统默认头像，跳过删除", currentUserId);
                }
            }
            // 2.2 上传新头像到OSS，获取新URL
            newAvatarUrl = ossFileUtil.uploadFile(avatar, ossAvatarPath);
            log.info("用户{}新头像上传OSS成功，URL：{}", currentUserId, newAvatarUrl);
        } catch (IOException e) {
            log.error("用户{}头像文件流异常", currentUserId, e);
            throw new RuntimeException("头像上传失败：文件流异常");
        } catch (IllegalArgumentException e) {
            log.error("用户{}头像参数非法", currentUserId, e);
            throw new RuntimeException("头像上传失败：" + e.getMessage());
        } catch (RuntimeException e) {
            log.error("用户{}OSS上传失败", currentUserId, e);
            throw new RuntimeException("头像上传失败：" + e.getMessage());
        }

        userMapper.updateAvatar(currentUserId, newAvatarUrl);
        cacheClient.delete(RedisConstants.CACHE_USER_DETAIL_KEY + currentUserId);
        clearFriendCacheForUser(currentUserId);

        return newAvatarUrl; // 返回新头像URL，供update接口使用
    }

    @Override
    public UserDetailVO updateUserDetail(Long currentUserId, UserUpdateDTO userUpdateDTO){
        User user = userMapper.selectById(currentUserId);
        if(user == null){
            throw new IllegalArgumentException("用户不存在或已被删除");
        }

        // 更新用户信息
        user.setNickname(userUpdateDTO.getNickname());
        user.setGender(GenderEnum.getCodeByText(userUpdateDTO.getGender()));
        user.setSignature(userUpdateDTO.getSignature());

        // 仅当avatarUrl不为空时更新（用户可能只改昵称，不改头像）
        if (userUpdateDTO.getAvatar() != null && !userUpdateDTO.getAvatar().trim().isEmpty()) {
            user.setAvatar(userUpdateDTO.getAvatar());
            log.info("用户{}更新头像URL：{}", currentUserId, userUpdateDTO.getAvatar());
        }

        // 更新数据库
        userMapper.updateById(user);

        UserDetailVO userDetailVO = UserDetailVO.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .gender(userUpdateDTO.getGender())
                .signature(user.getSignature())
                .createdTime(user.getCreatedTime())
                .build();
        cacheClient.delete(RedisConstants.CACHE_USER_DETAIL_KEY + currentUserId);
        clearFriendCacheForUser(currentUserId);

        return userDetailVO;
    }

    private void clearFriendCacheForUser(Long userId) {
        List<Long> ownerIds = friendMapper.selectUserIdsByFriendId(userId);
        if (ownerIds == null || ownerIds.isEmpty()) {
            return;
        }
        List<String> keys = new ArrayList<>();
        for (Long ownerId : ownerIds) {
            keys.add(RedisConstants.CACHE_FRIEND_LIST_KEY_PREFIX + ownerId);
            keys.add(RedisConstants.CACHE_FRIEND_DETAIL_KEY_PREFIX + ownerId + ":" + userId);
        }
        cacheClient.deleteBatch(keys);
    }

}
