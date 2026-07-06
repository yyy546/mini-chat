package com.minichat.space.constants;

public class FeedConstants {
    /**
     * 默认分页大小及最小时间戳
     */
    public static final long DEFAULT_PAGE_SIZE = 10L;
    public static final double MIN_SCORE_TIMESTAMP = 0.0;
    /**
     * 最大保留数量及对应修剪结束索引
     */
    public static final long FEED_MAX_KEEP_COUNT = 500L;
    public static final long FEED_TRIM_END_INDEX = -(FEED_MAX_KEEP_COUNT + 1);

    private FeedConstants() {}
}