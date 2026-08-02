package io.novafs.rag.splitter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextSplitterTest {

    private final TextSplitter splitter = new TextSplitter(20, 4);

    @Test
    void shortText_returnsSingleChunk() {
        List<String> chunks = splitter.split("你好世界");
        assertEquals(1, chunks.size());
        assertEquals("你好世界", chunks.get(0));
    }

    @Test
    void emptyText_returnsNoChunk() {
        assertTrue(splitter.split("   ").isEmpty());
    }

    @Test
    void longText_splitByParagraph() {
        String text = "第一段内容测试。" + "\n\n" + "第二段内容测试。" + "\n\n" + "第三段内容测试。";
        List<String> chunks = splitter.split(text);
        assertTrue(chunks.size() >= 2, "应切出至少 2 块,实际 " + chunks.size());
        assertTrue(chunks.stream().allMatch(c -> c.length() <= 20));
    }

    @Test
    void noSeparator_forceSplitBySize() {
        TextSplitter noOverlap = new TextSplitter(20, 0);
        String text = "a".repeat(100);
        List<String> chunks = noOverlap.split(text);
        assertEquals(5, chunks.size());
        assertTrue(chunks.stream().allMatch(c -> c.length() <= 20));
    }

    @Test
    void overlap_appliedBetweenAdjacentChunks() {
        String text = "a".repeat(100);
        List<String> chunks = splitter.split(text);
        assertEquals(5, chunks.size());
        for (int i = 1; i < chunks.size(); i++) {
            assertTrue(chunks.get(i).startsWith("aaaa"));
        }
    }

    @Test
    void invalidParams_rejected() {
        assertThrows(IllegalArgumentException.class, () -> new TextSplitter(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new TextSplitter(10, 10));
    }
}