package com.minichat.common.cache;

import com.alibaba.fastjson2.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.minichat.common.mq.MqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheClient {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final RabbitTemplate rabbitTemplate;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    private final Cache<String, Object> caffeineCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    //将数据缓存到redis，设置过期时间
    public void set(String key, Object data, Long expireTime, TimeUnit timeUnit) {
        putToCaffeine(key, data);
        putToRedis(key, data, expireTime, timeUnit);
    }

    //将数据缓存到redis，设置逻辑过期时间
    public void setWithLogicExpire(String key, Object data, Long expireTime, TimeUnit timeUnit) {
        RedisData redisData = RedisData.builder()
                .expireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(expireTime)))
                .data(data)
                .build();
        putToCaffeine(key, redisData);
        putToRedisWithLogicExpire(key, data, expireTime, timeUnit);
    }

    //根据key删除缓存
    public void delete(String key){
        invalidateCaffeine(key);
        deleteFromRedis(key);
        // 发送缓存删除消息到MQ
        rabbitTemplate.convertAndSend(MqConstants.CACHE_SYNC_SIMPLE_EXCHANGE,
                MqConstants.CACHE_SYNC_ROUTING_KEY,
                key);
    }

    //批量删除缓存
    public void deleteBatch(Collection<String> keys){
        invalidateCaffeineBatch(keys);
        deleteFromRedisBatch(keys);
        // 发送批量缓存删除消息到MQ
        rabbitTemplate.convertAndSend(MqConstants.CACHE_SYNC_BATCH_EXCHANGE,
                MqConstants.CACHE_SYNC_ROUTING_KEY,
                keys);
    }

    // 批量查询缓存（Redis multiGet 批量读取）
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> batchQuery(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        if (values == null) {
            return Collections.emptyMap();
        }
        Map<String, T> result = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            Object value = values.get(i);
            if (value != null) {
                result.put(keys.get(i), (T) value);
            }
        }
        return result;
    }

    // 按模式删除缓存（如 cache:friend:*:123）
    public void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            invalidateCaffeineBatch(keys);
            deleteFromRedisBatch(keys);
        }
    }

    // 解决缓存穿透问题（支持复杂类型，如 List<T>）
    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, TypeReference<R> typeReference,
                                          Function<ID, R> dbFallback, Long expireTime, TimeUnit timeUnit) {
        String key = keyPrefix + id;

        //查一级缓存
        Object localResult = getFromCaffeine(key);
        if(localResult != null){
            if(localResult instanceof String && ((String) localResult).isEmpty()){
                return null;
            }
            try {
                return (R) localResult;
            } catch (ClassCastException e) {
                // 类型不匹配，重新查询
                log.warn("Caffeine缓存类型不匹配，key：{}，原始类型：{}，目标类型：{}", key, localResult.getClass(), typeReference.getType());
                invalidateCaffeine(key);
            }
        }

        //查二级缓存
        Object RedisResult = getFromRedis(key);

        //查询到数据
        if(RedisResult != null) {
            // 空值处理 (假设存的是空字符串表示null)
            if (RedisResult instanceof String && ((String) RedisResult).isEmpty()) {
                putToCaffeine(key, "");
                return null;
            }
            try {
                putToCaffeine(key, RedisResult);
                return (R) RedisResult;
            } catch (ClassCastException e) {
                // 类型不匹配，重新查询
                log.warn("Redis缓存类型不匹配，key：{}，原始类型：{}，目标类型：{}", key, RedisResult.getClass(), typeReference.getType());
                invalidateCaffeine(key);
                deleteFromRedis(key);
            }
        }

        //从数据库中查询数据
        R dbResult = dbFallback.apply(id);
        //如果数据库中也没有数据，将空值缓存到一级缓存和二级缓存
        if(dbResult == null) {
            set(key, "", CacheKeys.EXPIRE_NULL_SECONDS, TimeUnit.SECONDS);
            return null;
        }
        //有数据，将数据库中的数据缓存到一级缓存和二级缓存
        set(key, dbResult, expireTime, timeUnit);
        return dbResult;
    }

    // 解决缓存击穿问题（支持复杂类型，如 List<T>），使用逻辑过期时间
    public <R, ID> R queryWithLogicalExpire(String keyPrefix, ID id, TypeReference<R> typeReference,
                                            Function<ID, R> dbFallback, Long time, TimeUnit timeUnit) {
        String key = keyPrefix +id;

        //查一级缓存
        Object localResult = getFromCaffeine(key);
        if(localResult != null){
            if(localResult instanceof String && ((String) localResult).isEmpty()){
                return null;
            }
            try {
                RedisData redisData = (RedisData) localResult;
                LocalDateTime expireTime = redisData.getExpireTime();
                //判断逻辑时间是否过期
                if(expireTime.isAfter(LocalDateTime.now())) {
                    //未过期，直接返回数据
                    return (R) redisData.getData();
                }
                //过期，需要缓存重建
                rebuildCache(key, id, dbFallback, time, timeUnit);
            } catch (ClassCastException e) {
                // 类型不匹配，重新查询
                log.warn("Caffeine逻辑过期缓存类型不匹配，key：{}，原始类型：{}，目标类型：{}", key, localResult.getClass(), typeReference.getType());
                caffeineCache.invalidate(key);
            }
        }

        //查二级缓存
        //从redis中查询数据
        Object RedisResult = getFromRedis(key);

        //未查询到数据，返回null
        if(RedisResult == null){
            return null;
        }

        //反序列化
        RedisData redisData;
        try {
            redisData = (RedisData) RedisResult;
        } catch (ClassCastException e) {
            log.warn("Redis逻辑过期缓存类型不匹配，key：{}，原始类型：{}，目标类型：{}", key, RedisResult.getClass(), typeReference.getType());
            delete(key);
            return null;
        }

        R r = (R) redisData.getData();
        LocalDateTime expireTime = redisData.getExpireTime();

        //同步到本地缓存
        putToCaffeine(key, redisData);

        //判断逻辑时间是否过期
        if(expireTime.isAfter(LocalDateTime.now())) {
            //未过期，直接返回数据
            return r;
        }

        //过期，需要缓存重建
        rebuildCache(key, id, dbFallback, time, timeUnit);

        //未获取到锁，直接返回旧数据
        return r;
    }

    /**
     * 缓存重建
     */
    private <ID, R> void rebuildCache(String key, ID id, Function<ID, R> dbFallback, Long time, TimeUnit timeUnit) {
        String lockKey = CacheKeys.lock(key);
        CACHE_REBUILD_EXECUTOR.submit(() -> {
            RLock lock = redissonClient.getLock(lockKey);
            boolean isLock;
            try {
                isLock = lock.tryLock(CacheKeys.LOCK_WAIT_SECONDS, CacheKeys.LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("Redis锁获取异常，key：{}，原因：{}", lockKey, e.getMessage(), e);
                return;
            }
            if (!isLock) {
                return;
            }
            try {
                R newR = dbFallback.apply(id);
                setWithLogicExpire(key, newR, time, timeUnit);
                log.info("逻辑过期缓存重建成功，key：{}", key);
            }catch (Exception e){
                log.error("逻辑过期缓存重建失败，key：{}，原因：{}", key, e.getMessage(), e);
            }finally {
                lock.unlock();
            }
        });
    }

    /**
     * Caffeine相关操作
     */
    private Object getFromCaffeine(String key){
        try{
            Object result = caffeineCache.getIfPresent(key);
            if(result != null){
                log.info("Caffeine缓存命中，key：{}", key);
            }
            return result;
        }catch (Exception e){
            log.error("Caffeine缓存获取异常，key：{}，原因：{}", key, e.getMessage(), e);
            return null;
        }
    }

    private void putToCaffeine(String key, Object value){
        try {
            caffeineCache.put(key, value);
            log.info("Caffeine缓存设置成功，key：{}", key);
        }catch (Exception e){
            log.error("Caffeine缓存设置异常，key：{}，原因：{}", key, e.getMessage(), e);
        }
    }

    public void invalidateCaffeine(String key){
        try {
            caffeineCache.invalidate(key);
            log.info("Caffeine缓存清除成功，key：{}", key);
        }catch (Exception e){
            log.error("Caffeine缓存清除异常，key：{}，原因：{}", key, e.getMessage(), e);
        }
    }

    public void invalidateCaffeineBatch(Collection<String> keys){
        try {
            caffeineCache.invalidateAll(keys);
            log.info("Caffeine缓存批量清除成功，keys：{}", keys);
        }catch (Exception e){
            log.error("Caffeine缓存批量清除异常，keys：{}，原因：{}", keys, e.getMessage(), e);
        }
    }

    /**
     * Redis相关操作
     */
    private Object getFromRedis(String key){
        try{
            Object result = redisTemplate.opsForValue().get(key);
            if(result != null){
                log.info("Redis缓存命中，key：{}", key);
            }
            return result;
        }catch (Exception e){
            log.error("Redis缓存获取异常，key：{}，原因：{}", key, e.getMessage(), e);
            return null;
        }
    }

    private void putToRedis(String key, Object value, Long expireTime, TimeUnit timeUnit){
        try {
            redisTemplate.opsForValue().set(key, value, expireTime, timeUnit);
            log.info("Redis缓存设置成功，key：{}", key);
        }catch (Exception e){
            log.error("Redis缓存设置异常，key：{}，原因：{}", key, e.getMessage(), e);
        }
    }

    private void putToRedisWithLogicExpire(String key, Object value, Long expireTime, TimeUnit timeUnit){
        try {
            RedisData redisData = RedisData.builder()
                    .expireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(expireTime)))
                    .data(value)
                    .build();
            redisTemplate.opsForValue().set(key, redisData);
            log.info("Redis逻辑过期缓存设置成功，key：{}", key);
        }catch (Exception e){
            log.error("Redis逻辑过期缓存设置异常，key：{}，原因：{}", key, e.getMessage(), e);
        }
    }

    private void deleteFromRedis(String key){
        try {
            redisTemplate.delete(key);
            log.info("Redis缓存清除成功，key：{}", key);
        }catch (Exception e){
            log.error("Redis缓存清除异常，key：{}，原因：{}", key, e.getMessage(), e);
        }
    }

    private void deleteFromRedisBatch(Collection<String> keys){
        try {
            redisTemplate.delete(keys);
            log.info("Redis缓存批量清除成功，keys：{}", keys);
        }catch (Exception e){
            log.error("Redis缓存批量清除异常，keys：{}，原因：{}", keys, e.getMessage(), e);
        }
    }

}
