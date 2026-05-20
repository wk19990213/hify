package com.hify.knowledge.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 文本分块器 —— 按字符数切分，保留重叠 */
@Component
public class TextSplitter {

    public record Chunk(int index, String content) {}

    /** 按字符数切分，相邻块重叠 overlap 个字符 */
    public List<Chunk> split(String text, int chunkSize, int overlap) {
        List<Chunk> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;

        int start = 0;
        int idx = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            if (end < text.length()) {
                int breakPoint = findBreakPoint(text, start, end);
                if (breakPoint > start) end = breakPoint;
            }
            chunks.add(new Chunk(idx++, text.substring(start, end).trim()));
            if (end >= text.length()) break; // 已到末尾
            start = end - overlap;
            if (start >= text.length() || start <= 0) break;
        }
        return chunks;
    }

    /** 在 [start, end) 范围内找最佳断点：换行 > 句号 > 逗号 */
    private int findBreakPoint(String text, int start, int end) {
        int windowStart = Math.max(start, end - 100);
        for (int i = end - 1; i >= windowStart; i--) {
            char c = text.charAt(i);
            if (c == '\n') return i + 1;
        }
        for (int i = end - 1; i >= windowStart; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '.') return i + 1;
        }
        return end;
    }
}
