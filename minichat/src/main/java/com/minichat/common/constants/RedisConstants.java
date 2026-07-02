package com.minichat.common.constants;

import com.minichat.common.cache.CacheKeys;

/**
 * @deprecated 使用 {@link CacheKeys} 替代。本类保留仅为向后兼容，新代码请使用 CacheKeys。
 */
@Deprecated
public class RedisConstants {

    @Deprecated
    public static final String USER_ONLINE_USER_KEY_PREFIX = CacheKeys.USER_ONLINE_PREFIX;
    @Deprecated
    public static final String STATUS_TOPIC = CacheKeys.STATUS_TOPIC;
    @Deprecated
    public static final Long ONLINE_STATUS_EXPIRE_TIME = CacheKeys.EXPIRE_ONLINE_SECONDS;

    @Deprecated
    public static final String GROUP_MESSAGE_SEQ_KEY_PREFIX = CacheKeys.GROUP_MESSAGE_SEQ_PREFIX;

    @Deprecated
    public static final Long CACHE_NULL_EXPIRE_TIME = CacheKeys.EXPIRE_NULL_SECONDS;
    @Deprecated
    public static final Long CACHE_NORMAL_EXPIRE_TIME = CacheKeys.EXPIRE_NORMAL;
    @Deprecated
    public static final Long LOCK_EXPIRE_TIME = CacheKeys.LOCK_EXPIRE_SECONDS;
    @Deprecated
    public static final Long WAIT_TIME = CacheKeys.LOCK_WAIT_SECONDS;

    @Deprecated
    public static final String CACHE_USER_DETAIL_KEY = CacheKeys.USER_DETAIL_PREFIX;
    @Deprecated
    public static final String CACHE_FRIEND_LIST_KEY_PREFIX = CacheKeys.FRIEND_LIST_PREFIX;
    @Deprecated
    public static final String CACHE_FRIEND_GROUP_KEY_PREFIX = CacheKeys.FRIEND_GROUP_PREFIX;
    @Deprecated
    public static final String CACHE_FRIEND_DETAIL_KEY_PREFIX = CacheKeys.FRIEND_DETAIL_PREFIX;
    @Deprecated
    public static final String CACHE_USER_GROUP_LIST_KEY_PREFIX = CacheKeys.USER_GROUP_LIST_PREFIX;
    @Deprecated
    public static final String CACHE_GROUP_PROFILE_KEY_PREFIX = CacheKeys.GROUP_PROFILE_PREFIX;
    @Deprecated
    public static final String LOCK_KEY_PREFIX = "lock:";
    @Deprecated
    public static final String FEED_FOLLOWED_KEY_PREFIX = CacheKeys.FEED_FOLLOWED_PREFIX;
    @Deprecated
    public static final String LOCK_REGISTER_KEY_PREFIX = "lock:register:";

    private RedisConstants() {}
}
