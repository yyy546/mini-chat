package com.minichat.common.constants;

public class RedisConstants {
    // 在线用户状态键前缀
    public static final String USER_ONLINE_USER_KEY_PREFIX = "user:online:";
    // 状态推送的主题
    public static final String STATUS_TOPIC = "/topic/user-status";
    // 在线状态过期时间（秒）
    public static final Long ONLINE_STATUS_EXPIRE_TIME = 300L;

    public static final String GROUP_MESSAGE_SEQ_KEY_PREFIX = "group_msg_seq:";

    // 缓存空值过期时间（秒）
    public static final Long CACHE_NULL_EXPIRE_TIME = 30L;
    // 缓存正常过期时间（分钟）
    public static final Long CACHE_NORMAL_EXPIRE_TIME = 10L;
    // 缓存锁过期时间（秒）
    public static final Long LOCK_EXPIRE_TIME = 30L;
    // 缓存锁等待时间（秒）
    public static final Long WAIT_TIME = 1L;
    /**
     * 用户详情缓存键前缀
     */

    public static final String CACHE_USER_DETAIL_KEY = "cache:user:detail:";

    /**
     * 好友相关缓存键前缀
     */
    public static final String CACHE_FRIEND_LIST_KEY_PREFIX = "cache:friend:list:";
    public static final String CACHE_FRIEND_GROUP_KEY_PREFIX = "cache:friend:group:";
    public static final String CACHE_FRIEND_DETAIL_KEY_PREFIX = "cache:friend:detail:";

    /**
     * 群组相关缓存键前缀
     */
    public static final String CACHE_USER_GROUP_LIST_KEY_PREFIX = "cache:group:list:";
    public static final String CACHE_GROUP_PROFILE_KEY_PREFIX = "cache:group:profile:";
    public static final String LOCK_KEY_PREFIX = "lock:";

    /**
     * 动态相关缓存键前缀
     */
    public static final String FEED_FOLLOWED_KEY_PREFIX = "feed:followed:";

     /**
     * 注册锁键前缀
     */
    public static final String LOCK_REGISTER_KEY_PREFIX = LOCK_KEY_PREFIX + "register:";


    private RedisConstants(){}
}
