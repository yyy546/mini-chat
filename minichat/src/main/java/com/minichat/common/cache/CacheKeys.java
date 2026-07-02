package com.minichat.common.cache;

/**
 * 统一缓存 Key 管理 —— 所有缓存 Key 的静态工厂方法，按模块分区。
 * 禁止在业务代码中硬编码拼接 Key 字符串。
 */
public final class CacheKeys {

    // ===== 统一过期时间（分钟） =====
    public static final long EXPIRE_SHORT = 5;
    public static final long EXPIRE_NORMAL = 60;
    public static final long EXPIRE_LONG = 360;

    // ===== 特殊过期时间 =====
    public static final long EXPIRE_NULL_SECONDS = 30;
    public static final long EXPIRE_ONLINE_SECONDS = 300;
    public static final long LOCK_WAIT_SECONDS = 1;
    public static final long LOCK_EXPIRE_SECONDS = 30;

    // ===== WebSocket 主题 =====
    public static final String STATUS_TOPIC = "/topic/user-status";

    // ===== 用户模块 =====
    public static final String USER_ONLINE_PREFIX = "user:online:";
    public static final String USER_DETAIL_PREFIX = "cache:user:detail:";

    public static String userOnline(Long userId) {
        return USER_ONLINE_PREFIX + userId;
    }

    public static String userDetail(Long userId) {
        return USER_DETAIL_PREFIX + userId;
    }

    // ===== 好友模块 =====
    public static final String FRIEND_LIST_PREFIX = "cache:friend:list:";
    public static final String FRIEND_GROUP_PREFIX = "cache:friend:group:";
    public static final String FRIEND_DETAIL_PREFIX = "cache:friend:detail:";

    public static String friendList(Long userId) {
        return FRIEND_LIST_PREFIX + userId;
    }

    public static String friendGroup(Long userId) {
        return FRIEND_GROUP_PREFIX + userId;
    }

    public static String friendDetail(Long userId, Long friendId) {
        return FRIEND_DETAIL_PREFIX + userId + ":" + friendId;
    }

    // ===== 群组模块 =====
    public static final String USER_GROUP_LIST_PREFIX = "cache:group:list:";
    public static final String GROUP_PROFILE_PREFIX = "cache:group:profile:";
    public static final String GROUP_MESSAGE_SEQ_PREFIX = "group_msg_seq:";

    public static String userGroupList(Long userId) {
        return USER_GROUP_LIST_PREFIX + userId;
    }

    public static String groupProfile(Long groupId) {
        return GROUP_PROFILE_PREFIX + groupId;
    }

    public static String groupMessageSeq(Long groupId) {
        return GROUP_MESSAGE_SEQ_PREFIX + groupId;
    }

    // ===== 空间模块 =====
    public static final String FEED_FOLLOWED_PREFIX = "feed:followed:";

    public static String feedFollowed(Long userId) {
        return FEED_FOLLOWED_PREFIX + userId;
    }

    // ===== 锁 =====
    private static final String LOCK_PREFIX = "lock:";

    public static String lock(String key) {
        return LOCK_PREFIX + key;
    }

    public static String lockRegister(String username) {
        return LOCK_PREFIX + "register:" + username;
    }

    private CacheKeys() {}
}
