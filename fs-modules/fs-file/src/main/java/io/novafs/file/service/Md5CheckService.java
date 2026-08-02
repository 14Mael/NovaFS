package io.novafs.file.service;

import com.mybatisflex.core.query.QueryWrapper;
import io.novafs.file.dto.CheckMd5Request;
import io.novafs.file.dto.CheckMd5Result;
import io.novafs.file.entity.FileInfo;
import io.novafs.file.mapper.FileInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 秒传校验服务
 */
@Service
@RequiredArgsConstructor
public class Md5CheckService {

    private final FileInfoMapper fileInfoMapper;

    /**
     * 同工作空间内校验 MD5 + 文件大小是否已存在（同时校验大小防 MD5 碰撞）
     */
    public CheckMd5Result check(CheckMd5Request request, Long workspaceId) {
        FileInfo file = fileInfoMapper.selectOneByQuery(
                QueryWrapper.create()
                        .where(FileInfo::getContentMd5).eq(request.getMd5())
                        .and(FileInfo::getSize).eq(request.getFileSize())
                        .and(FileInfo::getIsDeleted).eq(false)
                        .and(FileInfo::getWorkspaceId).eq(workspaceId)
                        .limit(1));

        CheckMd5Result result = new CheckMd5Result();
        if (file != null) {
            result.setExists(true);
            result.setFileId(file.getId());
            result.setFileName(file.getOriginalName());
            result.setFileSize(file.getSize());
        }
        return result;
    }
}
