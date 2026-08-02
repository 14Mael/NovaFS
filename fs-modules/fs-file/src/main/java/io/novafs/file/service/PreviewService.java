package io.novafs.file.service;

import io.novafs.file.dto.FilePreviewVO;
import io.novafs.file.entity.FileInfo;
import io.novafs.file.mapper.FileInfoMapper;
import io.novafs.file.preview.PreviewStrategyManager;
import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 文件预览服务
 */
@Service
@RequiredArgsConstructor
public class PreviewService {

    private final PreviewStrategyManager strategyManager;
    private final FileInfoMapper fileInfoMapper;

    public FilePreviewVO getPreviewInfo(Long fileId, Long userId) {
        FileInfo file = fileInfoMapper.selectOneById(fileId);
        if (file == null) {
            throw new BaseException(ErrorCode.FILE_NOT_FOUND);
        }
        if (!file.canBeDeletedBy(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN);
        }
        return strategyManager.match(file).preview(file, userId);
    }
}
