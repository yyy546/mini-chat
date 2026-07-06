package com.minichat.common.sensitive.engine;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class SensitiveWordService {

    private final RedisTemplate<String, Object> redisTemplate;
    private SensitiveWordFilter filter = new SensitiveWordFilter();
    private static final String SENSITIVE_WORDS_KEY = "minichat:sensitive:words";

    // 系统启动时加载
    @PostConstruct
    public void init() {
        loadFromRedis();
    }

    public void loadFromRedis() {
        Set<Object> words = redisTemplate.opsForSet().members(SENSITIVE_WORDS_KEY);
        SensitiveWordFilter newFilter = new SensitiveWordFilter();
        if (words != null) {
            words.forEach(word -> newFilter.addWord((String) word));
        }
        newFilter.buildFailPointers();
        this.filter = newFilter; // 保证原子性切换
        log.info("敏感词库加载完成，共 {} 个词", words != null ? words.size() : 0);
    }

    // 过滤方法
    public String filterText(String text, String replacement) {
        return filter.filter(text, replacement);
    }

    // 动态添加并同步 Redis
    public void addWord(String word) {
        redisTemplate.opsForSet().add(SENSITIVE_WORDS_KEY, word);
        loadFromRedis(); // 重新构建（或发送 MQ 通知其他实例更新）
    }
}
