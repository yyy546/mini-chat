package com.minichat.common.util.sensitive;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class TrieNode implements Serializable {
    private Map<Character, TrieNode> children = new ConcurrentHashMap<>();
    private boolean isEnd = false;
    private TrieNode fail;  // 失败指针
    private int length;     // 敏感词长度
}
