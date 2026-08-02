package io.novafs.rag.parser;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文档解析策略接口
 * <p>按文件名/MIME 类型匹配解析器,将文档内容提取为纯文本。</p>
 */
public interface DocumentParser {

    /** 是否支持解析该文档 */
    boolean supports(String fileName, String contentType);

    /** 解析为纯文本 */
    String parse(InputStream in) throws IOException;
}