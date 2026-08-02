package io.novafs.file.service;

import io.novafs.file.dto.ChunkUploadResponse;
import io.novafs.file.dto.UploadedChunksResponse;
import io.novafs.file.entity.FileChunkInfo;
import io.novafs.file.entity.FileTransferTask;
import io.novafs.file.enums.TransferTaskStatus;
import io.novafs.file.enums.TransferTaskType;
import io.novafs.file.event.ChunkUploadProgressEvent;
import io.novafs.file.mapper.FileChunkInfoMapper;
import io.novafs.file.mapper.FileTransferTaskMapper;
import io.novafs.file.storage.StorageConfigResolver;
import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import io.novafs.storage.plugin.boot.StorageServiceFacade;
import io.novafs.storage.plugin.core.model.StorageConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 分片上传服务测试（幂等 + 进度更新 + 断点续传查询）
 */
@ExtendWith(MockitoExtension.class)
class ChunkUploadServiceTest {

    @Mock
    private FileChunkInfoMapper chunkInfoMapper;
    @Mock
    private FileTransferTaskMapper taskMapper;
    @Mock
    private ChunkInitService chunkInitService;
    @Mock
    private StorageConfigResolver configResolver;
    @Mock
    private StorageServiceFacade storageFacade;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChunkUploadService service;

    @Test
    void shouldUploadChunkAndUpdateProgress() {
        FileTransferTask task = buildTask();
        when(chunkInfoMapper.selectCountByQuery(any())).thenReturn(0L);
        when(chunkInitService.requireTaskByUploadId("u1")).thenReturn(task);
        when(configResolver.resolve(any())).thenReturn(new StorageConfigResolver.StorageTarget("local", new StorageConfig()));

        ChunkUploadResponse response = service.upload("u1", 1, null,
                new ByteArrayInputStream("data".getBytes()), 4L);

        assertThat(response.isAlreadyExists()).isFalse();
        assertThat(response.getChunkNumber()).isEqualTo(1);
        verify(chunkInfoMapper).insert(argThat(c -> c.getChunkNumber() == 1 && c.getUploadId().equals("u1")));
        verify(taskMapper).incrementProgress(eq(1L), eq(4L));
        verify(eventPublisher).publishEvent(any(ChunkUploadProgressEvent.class));
    }

    @Test
    void shouldBeIdempotentForDuplicateChunk() {
        when(chunkInfoMapper.selectCountByQuery(any())).thenReturn(1L);

        ChunkUploadResponse response = service.upload("u1", 1, null,
                new ByteArrayInputStream("data".getBytes()), 4L);

        assertThat(response.isAlreadyExists()).isTrue();
        verify(storageFacade, never()).uploadChunk(any(), any(), any());
        verify(chunkInfoMapper, never()).insert(any());
    }

    @Test
    void shouldRejectUploadToCompletedTask() {
        FileTransferTask task = buildTask();
        task.markCompleted();
        when(chunkInfoMapper.selectCountByQuery(any())).thenReturn(0L);
        when(chunkInitService.requireTaskByUploadId("u1")).thenReturn(task);

        assertThatThrownBy(() -> service.upload("u1", 1, null,
                new ByteArrayInputStream("data".getBytes()), 4L))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getCode())
                .isEqualTo(ErrorCode.CONFLICT.getCode());
    }

    @Test
    void shouldListUploadedChunks() {
        FileTransferTask task = buildTask();
        task.setTotalChunks(3);
        task.setUploadedSize(8L);
        when(chunkInitService.requireTaskByUploadId("u1")).thenReturn(task);
        when(chunkInfoMapper.selectListByQuery(any())).thenReturn(List.of(chunk(1), chunk(2)));

        UploadedChunksResponse response = service.listUploadedChunks("u1");

        assertThat(response.getUploadedChunks()).containsExactly(1, 2);
        assertThat(response.getUploadedSize()).isEqualTo(8L);
        assertThat(response.getTotalChunks()).isEqualTo(3);
        assertThat(response.isCompleted()).isFalse();
    }

    private static FileTransferTask buildTask() {
        FileTransferTask task = new FileTransferTask();
        task.setId(1L);
        task.setTaskId("task-1");
        task.setUploadId("u1");
        task.setUserId(1L);
        task.setWorkspaceId(1L);
        task.setFileName("big.bin");
        task.setFileSize(1024L);
        task.setTotalChunks(3);
        task.setUploadedChunks(0);
        task.setUploadedSize(0L);
        task.setStatus(TransferTaskStatus.UPLOADING);
        task.setTaskType(TransferTaskType.UPLOAD);
        task.setStoragePlatformSettingId(1L);
        return task;
    }

    private static FileChunkInfo chunk(int number) {
        FileChunkInfo chunk = new FileChunkInfo();
        chunk.setUploadId("u1");
        chunk.setChunkNumber(number);
        return chunk;
    }
}
