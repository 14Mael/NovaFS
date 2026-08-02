package io.novafs.file.preview;

import io.novafs.file.dto.FilePreviewVO;
import io.novafs.file.entity.FileInfo;
import org.springframework.stereotype.Component;

/**
 * PDF 预览策略
 */
@Component
public class PdfPreviewStrategy implements PreviewStrategy {

    @Override
    public boolean support(FileInfo file) {
        return "pdf".equals(file.getSuffix());
    }

    @Override
    public int getOrder() {
        return 30;
    }

    @Override
    public FilePreviewVO preview(FileInfo file, Long userId) {
        FilePreviewVO vo = new FilePreviewVO();
        vo.setPreviewType("PDF");
        vo.setUrl(ImagePreviewStrategy.previewUrl(file));
        vo.setFileName(file.getOriginalName());
        return vo;
    }
}
