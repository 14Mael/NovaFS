package io.novafs.file.preview;

import io.novafs.file.dto.FilePreviewVO;
import io.novafs.file.entity.FileInfo;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 图片预览策略：直接返回预览地址
 */
@Component
public class ImagePreviewStrategy implements PreviewStrategy {

    private static final Set<String> IMAGE_SUFFIX = Set.of("png", "jpg", "jpeg", "gif", "webp", "svg", "bmp");

    @Override
    public boolean support(FileInfo file) {
        return file.getSuffix() != null && IMAGE_SUFFIX.contains(file.getSuffix());
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public FilePreviewVO preview(FileInfo file, Long userId) {
        FilePreviewVO vo = new FilePreviewVO();
        vo.setPreviewType("IMAGE");
        vo.setUrl(previewUrl(file));
        vo.setFileName(file.getOriginalName());
        return vo;
    }

    static String previewUrl(FileInfo file) {
        return "/api/file/preview/content/" + file.getId();
    }
}
