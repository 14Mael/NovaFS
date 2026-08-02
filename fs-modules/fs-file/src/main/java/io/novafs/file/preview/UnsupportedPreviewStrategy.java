package io.novafs.file.preview;

import io.novafs.file.dto.FilePreviewVO;
import io.novafs.file.entity.FileInfo;
import org.springframework.stereotype.Component;

/**
 * 兜底策略：不支持的格式
 */
@Component
public class UnsupportedPreviewStrategy implements PreviewStrategy {

    @Override
    public boolean support(FileInfo file) {
        return true;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public FilePreviewVO preview(FileInfo file, Long userId) {
        FilePreviewVO vo = new FilePreviewVO();
        vo.setPreviewType("UNSUPPORTED");
        vo.setFileName(file.getOriginalName());
        return vo;
    }
}
