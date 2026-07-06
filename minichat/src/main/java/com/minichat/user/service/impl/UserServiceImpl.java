package com.minichat.user.service.impl;

import com.alibaba.fastjson2.TypeReference;
import com.minichat.common.cache.CacheKeys;
import com.minichat.common.core.constants.OssConstants;
import com.minichat.user.constants.UserConstants;
import com.minichat.common.core.enums.GenderEnum;
import com.minichat.common.core.exception.ErrorCode;
import com.minichat.common.core.exception.UserException;
import com.minichat.common.cache.CacheClient;
import com.minichat.common.security.jwt.JwtUtil;
import com.minichat.common.core.util.OssFileUtil;
import com.minichat.friend.mapper.FriendMapper;
import com.minichat.user.dto.LoginDTO;
import com.minichat.user.dto.RegisterDTO;
import com.minichat.user.dto.UserUpdateDTO;
import com.minichat.user.entity.User;
import com.minichat.user.mapper.UserMapper;
import com.minichat.user.service.LoginAsyncService;
import com.minichat.user.service.UserService;
import com.minichat.user.vo.UserDetailVO;
import com.minichat.user.vo.UserLoginVO;
import com.minichat.user.vo.UserSearchVO;
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
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OssFileUtil ossFileUtil;
    private final CacheClient cacheClient;
    private final RedissonClient redissonClient;
    private final LoginAsyncService loginAsyncService;
    private final FriendMapper friendMapper;

    @Value("${aliyun.oss.avatar-path:avatar/}")
    private String ossAvatarPath;

    @Override
    @Transactional
    public void register(RegisterDTO registerDTO) {
        String username = registerDTO.getUsername();
        RLock lock = redissonClient.getLock(CacheKeys.lockRegister(username));
        try {
            if (!lock.tryLock(5, TimeUnit.SECONDS)) {
                throw new UserException(ErrorCode.INTERNAL_ERROR, "注册过于频繁，请稍后重试");
            }

            User existingUser = userMapper.selectByUsername(username);
            if (existingUser != null) {
                throw new UserException(ErrorCode.USER_ALREADY_EXISTS, "用户名已存在");
            }
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
            throw new UserException(ErrorCode.INTERNAL_ERROR, "注册失败，请稍后重试");
        } catch (DuplicateKeyException e) {
            log.warn("用户名重复注册，用户名：{}", registerDTO.getUsername(), e);
            throw new UserException(ErrorCode.USER_ALREADY_EXISTS, "用户名已存在");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }

    @Override
    public UserLoginVO login(LoginDTO loginDTO) {
        User user = userMapper.selectByUsername(loginDTO.getUsername());
        if (Objects.isNull(user)) {
            throw new UserException(ErrorCode.USER_NOT_FOUND, "用户名不存在");
        }
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new UserException(ErrorCode.USER_PASSWORD_ERROR, "密码错误");
        }
        if (UserConstants.DISABLE_STATUS.equals(user.getStatus())) {
            throw new UserException(ErrorCode.USER_DISABLED, "用户已被禁用");
        }
        Long userId = user.getId();
        loginAsyncService.asyncUpdateLastLoginTime(userId);
        String token = jwtUtil.generateToken(userId);
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
        if (Objects.isNull(user)) {
            throw new UserException(ErrorCode.USER_NOT_FOUND, "用户不存在或已被删除");
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
        if (Objects.isNull(user)) {
            throw new UserException(ErrorCode.USER_NOT_FOUND, "用户不存在或已被删除");
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
        if (currentUserId == null) {
            throw new UserException(ErrorCode.BAD_REQUEST, "用户ID不能为空");
        }

        UserDetailVO userDetailVO = cacheClient.queryWithPassThrough(CacheKeys.USER_DETAIL_PREFIX, currentUserId, new TypeReference<UserDetailVO>() {},
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
                CacheKeys.EXPIRE_NORMAL + new Random().nextLong(10), TimeUnit.MINUTES);

        return userDetailVO;
    }

    @Override
    public String uploadAvatar(Long currentUserId, MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            throw new UserException(ErrorCode.BAD_REQUEST, "头像文件不能为空");
        }
        User user = userMapper.selectById(currentUserId);
        if (user == null) {
            throw new UserException(ErrorCode.USER_NOT_FOUND, "用户不存在或已被删除");
        }

        String newAvatarUrl = null;
        try {
            String oldAvatarUrl = user.getAvatar();
            if (StringUtils.isNotBlank(oldAvatarUrl)) {
                if (!Objects.equals(oldAvatarUrl, OssConstants.DEFAULT_AVATAR_URL)) {
                    ossFileUtil.deleteFile(oldAvatarUrl);
                    log.info("用户{}旧头像（非默认）已从OSS删除，URL：{}", currentUserId, oldAvatarUrl);
                } else {
                    log.info("用户{}旧头像为系统默认头像，跳过删除", currentUserId);
                }
            }
            newAvatarUrl = ossFileUtil.uploadFile(avatar, ossAvatarPath);
            log.info("用户{}新头像上传OSS成功，URL：{}", currentUserId, newAvatarUrl);
        } catch (IOException e) {
            log.error("用户{}头像文件流异常", currentUserId, e);
            throw new UserException(ErrorCode.INTERNAL_ERROR, "头像上传失败：文件流异常");
        }

        userMapper.updateAvatar(currentUserId, newAvatarUrl);
        cacheClient.delete(CacheKeys.userDetail(currentUserId));
        clearFriendCacheForUser(currentUserId);

        return newAvatarUrl;
    }

    @Override
    public UserDetailVO updateUserDetail(Long currentUserId, UserUpdateDTO userUpdateDTO) {
        User user = userMapper.selectById(currentUserId);
        if (user == null) {
            throw new UserException(ErrorCode.USER_NOT_FOUND, "用户不存在或已被删除");
        }

        user.setNickname(userUpdateDTO.getNickname());
        user.setGender(GenderEnum.getCodeByText(userUpdateDTO.getGender()));
        user.setSignature(userUpdateDTO.getSignature());

        if (userUpdateDTO.getAvatar() != null && !userUpdateDTO.getAvatar().trim().isEmpty()) {
            user.setAvatar(userUpdateDTO.getAvatar());
            log.info("用户{}更新头像URL：{}", currentUserId, userUpdateDTO.getAvatar());
        }

        userMapper.updateById(user);

        UserDetailVO userDetailVO = UserDetailVO.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .gender(userUpdateDTO.getGender())
                .signature(user.getSignature())
                .createdTime(user.getCreatedTime())
                .build();
        cacheClient.delete(CacheKeys.userDetail(currentUserId));
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
            keys.add(CacheKeys.friendList(ownerId));
            keys.add(CacheKeys.friendDetail(ownerId, userId));
        }
        cacheClient.deleteBatch(keys);
    }

}
