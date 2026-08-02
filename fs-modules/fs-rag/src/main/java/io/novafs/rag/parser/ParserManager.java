package io.novafs.rag.parser;

import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 文档解析管理器:按解析器声明顺序选择第一个匹配的策略
 */
@Component
@RequiredArgsConstructor
public class ParserManager {

    private final List<DocumentParser> parsers;

    /** 解析文档为纯文本,不支持的格式抛出业务异常 */
    public String parse(String fileName, String contentType, InputStream in) {
        for (DocumentParser parser : parsers) {
            if (parser.supports(fileName, contentType)) {
                try {
                    return parser.parse(in);
                } catch (IOException e) {
                    throw new BaseException(ErrorCode.RAG_DOCUMENT_PARSE_FAILED, "文档解析失败: " + fileName);
                }
            }
        }
        throw new BaseException(ErrorCode.RAG_DOCUMENT_PARSE_FAILED, "不支持的文档类型: " + fileName);
    }
}