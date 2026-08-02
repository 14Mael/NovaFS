package io.novafs.file.preview;

import io.novafs.file.dto.FilePreviewVO;
import io.novafs.file.entity.FileInfo;
import io.novafs.file.service.FileInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 文本预览策略：读取文本内容返回（截断 64KB）
 */
@Component
@RequiredArgsConstructor
public class TextPreviewStrategy implements PreviewStrategy {

    private static final int MAX_TEXT_BYTES = 64 * 1024;

    private static final Set<String> TEXT_SUFFIX =
            Set.of("txt", "md", "log", "json", "xml", "yml", "yaml", "sql", "properties",
                    "java", "kt", "go", "py", "ts", "js", "tsx", "jsx", "css", "html", "htm", "sh");

    private final FileInfoService fileInfoService;

    @Override
    public boolean support(FileInfo file) {
        return file.getSuffix() != null && TEXT_SUFFIX.contains(file.getSuffix());
    }

    @Override
    public int getOrder() {
        return 40;
    }

    @Override
    public FilePreviewVO preview(FileInfo file, Long userId) {
        FilePreviewVO vo = new FilePreviewVO();
        vo.setPreviewType("TEXT");
        vo.setFileName(file.getOriginalName());
        try (InputStream in = fileInfoService.download(file.getId(), userId)) {
            byte[] bytes = in.readNBytes(MAX_TEXT_BYTES);
            vo.setContent(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException e) {
            vo.setContent("读取文本内容失败");
        }
        return vo;
    }
}
