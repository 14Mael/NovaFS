package io.novafs.file.service;

import com.mybatisflex.core.query.QueryWrapper;
import io.novafs.file.entity.FileTransferTask;
import io.novafs.file.enums.TransferTaskStatus;
import io.novafs.file.mapper.FileTransferTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 任务失败标记（REQUIRES_NEW 独立事务）
 * <p>分片合并失败时，主事务回滚会让任务停留在 UPLOADING，
 * 断点续传复用逻辑会反复复用 chunk 目录已丢失的僵尸任务。
 * 通过独立事务标记 FAILED，使下次上传创建全新任务。</p>
 */
@Service
@RequiredArgsConstructor
public class TaskFailureMarker {

    private final FileTransferTaskMapper taskMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String uploadId) {
        FileTransferTask task = taskMapper.selectOneByQuery(
                QueryWrapper.create().where(FileTransferTask::getUploadId).eq(uploadId));
        if (task != null && !task.isCompleted()) {
            task.setStatus(TransferTaskStatus.FAILED);
            taskMapper.update(task);
        }
    }
}
