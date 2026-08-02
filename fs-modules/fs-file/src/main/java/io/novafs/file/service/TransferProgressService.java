package io.novafs.file.service;

import com.mybatisflex.core.query.QueryWrapper;
import io.novafs.file.entity.FileTransferTask;
import io.novafs.file.mapper.FileTransferTaskMapper;
import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 传输进度服务：计算并组装上传进度数据
 */
@Service
@RequiredArgsConstructor
public class TransferProgressService {

    private final FileTransferTaskMapper taskMapper;

    /**
     * 查询任务进度
     */
    public Map<String, Object> progress(String uploadId) {
        FileTransferTask task = taskMapper.selectOneByQuery(
                QueryWrapper.create().where(FileTransferTask::getUploadId).eq(uploadId));
        if (task == null) {
            throw new BaseException(ErrorCode.FILE_NOT_FOUND, "上传任务不存在: " + uploadId);
        }
        Map<String, Object> progress = new LinkedHashMap<>();
        progress.put("uploadId", uploadId);
        progress.put("uploadedChunks", task.getUploadedChunks());
        progress.put("totalChunks", task.getTotalChunks());
        progress.put("uploadedSize", task.getUploadedSize());
        progress.put("fileSize", task.getFileSize());
        long fileSize = task.getFileSize() == null ? 0 : task.getFileSize();
        long uploadedSize = task.getUploadedSize() == null ? 0 : task.getUploadedSize();
        progress.put("percent", fileSize > 0 ? uploadedSize * 100 / fileSize : 0);
        return progress;
    }
}
