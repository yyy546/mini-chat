package com.minichat.common.sensitive.engine;

import io.jsonwebtoken.lang.Assert;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

@Slf4j
public class SensitiveWordFilter {

    private final TrieNode root = new TrieNode();

    public void addWord(String word) {
        TrieNode current = root;
        for( char c : word.toCharArray() ) {
            current = current.getChildren().computeIfAbsent(c, k -> new TrieNode());
        }
        current.setEnd(true);
        current.setLength(word.length());
    }

    public void buildFailPointers(){
        Queue<TrieNode> queue = new LinkedList<>();
        for(TrieNode node : root.getChildren().values()) {
            node.setFail(root);
            queue.add(node);
        }
        while(!queue.isEmpty()) {
            TrieNode parent = queue.poll();
            for(Map.Entry<Character, TrieNode> entry : parent.getChildren().entrySet()) {
                char c = entry.getKey();
                TrieNode child = entry.getValue();
                TrieNode fail = parent.getFail();
                while (fail != null && !fail.getChildren().containsKey(c)) {
                    fail = fail.getFail();
                }
                child.setFail(fail == null ? root : fail.getChildren().get(c));
                queue.add(child);
            }
        }
    }

    public String filter(String text, String replacement) {
        Assert.notNull(text, "text must not be null");
        StringBuilder result = new StringBuilder(text);
        TrieNode current = root;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // 如果是符号，跳过匹配但保持当前 Trie 状态
            if (isSymbol(c)) {
                continue;
            }

            // 沿失败指针找，直到找到匹配子节点或回到根
            while (current != root && !current.getChildren().containsKey(c)) {
                current = current.getFail();
            }

            // 检查当前节点是否有该字符的子节点
            current = current.getChildren().get(c);
            if (current == null) {
                current = root;
            }

            TrieNode temp = current;
            while (temp != root) {
                if (temp.isEnd()) {
                    // 找到匹配，回溯计算包含符号的实际开始位置
                    int matchLen = temp.getLength();
                    int actualStart = i;
                    int foundCount = 0;
                    while (actualStart >= 0 && foundCount < matchLen) {
                        if (!isSymbol(text.charAt(actualStart))) {
                            foundCount++;
                        }
                        if (foundCount < matchLen) {
                            actualStart--;
                        }
                    }
                    // 替换从 actualStart 到 i 的所有字符
                    for (int j = actualStart; j <= i; j++) {
                        result.setCharAt(j, replacement.charAt(0));
                    }
                }
                temp = temp.getFail();
            }
        }
        return result.toString();
    }

    /**
     * 判断是否为符号/空格（非字母、非数字）
     */
    private boolean isSymbol(char c) {
        return !Character.isLetterOrDigit(c) && (c < 0x2E80 || c > 0x9FFF);
    }
}
