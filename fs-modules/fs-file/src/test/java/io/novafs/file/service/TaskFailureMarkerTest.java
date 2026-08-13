package io.novafs.file.service;

import io.novafs.file.entity.FileTransferTask;
import io.novafs.file.enums.TransferTaskStatus;
import io.novafs.file.mapper.FileTransferTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 任务失败标记测试：独立事务将失败任务标记为 FAILED，防止断点续传复用僵尸任务
 */
@ExtendWith(MockitoExtension.class)
class TaskFailureMarkerTest {

    @Mock
    private FileTransferTaskMapper taskMapper;

    @InjectMocks
    private TaskFailureMarker marker;

    @Test
    void shouldMarkUploadingTaskFailed() {
        FileTransferTask task = new FileTransferTask();
        task.setUploadId("u1");
        task.setStatus(TransferTaskStatus.UPLOADING);
        when(taskMapper.selectOneByQuery(any())).thenReturn(task);

        marker.markFailed("u1");

        assertThat(task.getStatus()).isEqualTo(TransferTaskStatus.FAILED);
        verify(taskMapper).update(task);
    }

    @Test
    void shouldNotOverwriteCompletedTask() {
        FileTransferTask task = new FileTransferTask();
        task.setUploadId("u1");
        task.setStatus(TransferTaskStatus.COMPLETED);
        when(taskMapper.selectOneByQuery(any())).thenReturn(task);

        marker.markFailed("u1");

        assertThat(task.getStatus()).isEqualTo(TransferTaskStatus.COMPLETED);
        verify(taskMapper, never()).update(any());
    }
}
