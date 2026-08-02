package io.novafs.file.preview;

import io.novafs.file.dto.FilePreviewVO;
import io.novafs.file.entity.FileInfo;

/**
 * 文件预览策略
 */
public interface PreviewStrategy {

    /**
     * 是否支持该文件
     */
    boolean support(FileInfo file);

    /**
     * 优先级（数值越小越优先）
     */
    int getOrder();

    /**
     * 生成预览信息
     */
    FilePreviewVO preview(FileInfo file, Long userId);
}
