package com.minichat.space.service;

import com.minichat.common.core.result.ScrollResult;

public interface FeedService {

    /**
     * 获取动态流
     */
    ScrollResult feed(Long maxTimeStamp, Long offset);
}
