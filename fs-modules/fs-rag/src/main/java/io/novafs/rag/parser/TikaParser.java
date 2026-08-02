package io.novafs.rag.parser;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Tika 解析器:兜底处理 pdf / docx / xlsx / pptx / rtf 等二进制格式
 * <p>{@link Tika} 线程安全,复用单例。</p>
 */
@Component
public class TikaParser implements DocumentParser {

    private final Tika tika = new Tika();

    @Override
    public boolean supports(String fileName, String contentType) {
        // 作为兜底解析器,总是支持
        return true;
    }

    @Override
    public String parse(InputStream in) throws IOException {
        try {
            return tika.parseToString(in);
        } catch (TikaException e) {
            throw new IOException("Tika 解析失败", e);
        }
    }
}