package com.minichat.space.service;

import com.minichat.common.result.Result;
import com.minichat.common.result.ScrollResult;

public interface FeedService {

    /**
     * 获取动态流
     */
    Result<ScrollResult> feed(Long maxTimeStamp, Long offset);
}
