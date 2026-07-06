package com.minichat.common.util.sensitive;

import com.minichat.common.sensitive.engine.SensitiveWordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class SensitiveWordTest {

    @Autowired
    private SensitiveWordService sensitiveWordService;

    /**
     * 向 Redis 中添加敏感词并刷新本地 Trie 树
     */
    @Test
    public void testAddSensitiveWords() {
        List<String> words = Arrays.asList("敏感词1", "敏感词2", "不健康内容", "违禁品");
        for (String word : words) {
            sensitiveWordService.addWord(word);
        }
        System.out.println("敏感词添加成功并已同步到本地缓存");
    }

    /**
     * 测试敏感词过滤效果
     */
    @Test
    public void testFilter() {
        String originalText = "你好，这是一段包含敏感词 1和违禁品的测试文本。";
        String filteredText = sensitiveWordService.filterText(originalText, "***");
        
        System.out.println("原文本: " + originalText);
        System.out.println("过滤后: " + filteredText);
        
        // 断言过滤后文本不包含原敏感词（可选）
        assert !filteredText.contains("敏感词1");
        assert !filteredText.contains("违禁品");
    }
}
