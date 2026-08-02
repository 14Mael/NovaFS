package io.novafs.rag.splitter;

import java.util.ArrayList;
import java.util.List;

/**
 * 递归字符文本切片器
 * <p>按段落 → 换行 → 句号 → 空格 的优先级切分,块大小与相邻块重叠均可配置。</p>
 */
public class TextSplitter {

    private static final String[] SEPARATORS = {"\n\n", "\n", "。", "！", "？", ". ", " "};

    private final int chunkSize;
    private final int chunkOverlap;

    public TextSplitter(int chunkSize, int chunkOverlap) {
        if (chunkSize <= 0 || chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("chunkSize 必须为正数且 chunkOverlap 必须小于 chunkSize");
        }
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    /** 将文本切分为片段列表 */
    public List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        splitRecursive(text == null ? "" : text.trim(), 0, chunks);
        applyOverlap(chunks);
        return chunks;
    }

    private void splitRecursive(String text, int level, List<String> out) {
        if (text.isEmpty()) {
            return;
        }
        if (text.length() <= chunkSize) {
            out.add(text);
            return;
        }
        if (level >= SEPARATORS.length) {
            // 无合适分隔符,按固定长度硬切
            out.add(text.substring(0, chunkSize));
            splitRecursive(text.substring(chunkSize), level, out);
            return;
        }
        int cut = lastSeparatorWithin(text, SEPARATORS[level]);
        if (cut < 0) {
            // 当前层级没有分隔符,降级到下一层
            splitRecursive(text, level + 1, out);
            return;
        }
        splitRecursive(text.substring(0, cut), 0, out);
        splitRecursive(text.substring(cut), 0, out);
    }

    /** 返回不超过 chunkSize 的最后一个分隔符结束位置,找不到返回 -1 */
    private int lastSeparatorWithin(String text, String sep) {
        int searchFrom = 0;
        int last = -1;
        while (true) {
            int idx = text.indexOf(sep, searchFrom);
            if (idx < 0 || idx > chunkSize) {
                break;
            }
            last = idx;
            searchFrom = idx + sep.length();
        }
        return last;
    }

    private void applyOverlap(List<String> chunks) {
        if (chunkOverlap == 0 || chunks.size() < 2) {
            return;
        }
        for (int i = 1; i < chunks.size(); i++) {
            String prev = chunks.get(i - 1);
            String tail = prev.length() <= chunkOverlap ? prev : prev.substring(prev.length() - chunkOverlap);
            chunks.set(i, tail + chunks.get(i));
        }
    }
}