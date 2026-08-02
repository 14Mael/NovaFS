package io.novafs.file.listener;

import com.mybatisflex.core.query.QueryWrapper;
import io.novafs.file.entity.FileTransferTask;
import io.novafs.file.event.ChunkUploadProgressEvent;
import io.novafs.file.mapper.FileTransferTaskMapper;
import io.novafs.file.service.TransferProgressService;
import io.novafs.framework.sse.SseConnectionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 上传进度监听器：分片上传后通过 SSE 推送进度
 */
@Component
@RequiredArgsConstructor
public class UploadProgressListener {

    private final SseConnectionManager sseManager;
    private final TransferProgressService progressService;
    private final FileTransferTaskMapper taskMapper;

    @EventListener
    public void onChunkUploaded(ChunkUploadProgressEvent event) {
        FileTransferTask task = taskMapper.selectOneByQuery(
                QueryWrapper.create().where(FileTransferTask::getUploadId).eq(event.getUploadId()));
        if (task == null) {
            return;
        }
        sseManager.send(task.getUserId(), "upload-progress", progressService.progress(event.getUploadId()));
    }
}
