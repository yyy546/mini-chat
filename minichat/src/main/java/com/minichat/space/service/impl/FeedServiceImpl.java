package com.minichat.space.service.impl;

import com.minichat.common.cache.CacheKeys;
import com.minichat.common.constants.FeedConstants;
import com.minichat.common.result.ScrollResult;
import com.minichat.common.util.UserContext;
import com.minichat.space.mapper.SpacePostMapper;
import com.minichat.space.service.FeedService;
import com.minichat.space.vo.SpacePostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SpacePostMapper spacePostMapper;

    @Override
    public ScrollResult feed(Long maxTimeStamp, Long offset) {
        Long currentUserId = UserContext.getCurUserId();
        String key = CacheKeys.feedFollowed(currentUserId);
        Set<ZSetOperations.TypedTuple<Object>> typedTuples = redisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, FeedConstants.MIN_SCORE_TIMESTAMP, maxTimeStamp, offset, FeedConstants.DEFAULT_PAGE_SIZE);
        if (typedTuples == null || typedTuples.isEmpty()) {
            return new ScrollResult();
        }
        List<Long> ids = typedTuples.stream()
                .map(typedTuple -> {
                    Object value = typedTuple.getValue();
                    if (value instanceof Integer) {
                        return ((Integer) value).longValue();
                    }
                    return (Long) value;
                })
                .toList();
        long minTimestamp = 0L;
        int offsetSize = 1;
        if (!typedTuples.isEmpty()) {
            ZSetOperations.TypedTuple<Object> lastTuple = (ZSetOperations.TypedTuple<Object>) typedTuples.toArray()[typedTuples.size() - 1];
            minTimestamp = lastTuple.getScore().longValue();

            offsetSize = 0;
            for (ZSetOperations.TypedTuple<Object> typedTuple : typedTuples) {
                if (typedTuple.getScore().longValue() == minTimestamp) {
                    offsetSize++;
                }
            }
        }
        List<SpacePostVO> spacePostVOList = spacePostMapper.selectListByIds(ids, currentUserId);
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(spacePostVOList);
        scrollResult.setMinTime(minTimestamp);
        scrollResult.setOffset(offsetSize);
        return scrollResult;
    }
}
