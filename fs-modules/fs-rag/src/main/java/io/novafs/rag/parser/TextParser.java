package io.novafs.rag.parser;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * 纯文本解析器:txt / md / json / csv / 代码等,直接按 UTF-8 读取
 */
@Component
public class TextParser implements DocumentParser {

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "txt", "md", "markdown", "json", "csv", "yaml", "yml", "log",
            "xml", "html", "htm", "sql", "properties", "ini",
            "java", "py", "js", "ts", "go", "c", "cpp", "h", "sh");

    @Override
    public boolean supports(String fileName, String contentType) {
        if (contentType != null && contentType.startsWith("text/")) {
            return true;
        }
        return TEXT_EXTENSIONS.contains(extensionOf(fileName));
    }

    @Override
    public String parse(InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}