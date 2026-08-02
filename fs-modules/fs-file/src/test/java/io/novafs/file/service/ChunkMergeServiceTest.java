package io.novafs.file.service;

import io.novafs.file.dto.ChunkMergeRequest;
import io.novafs.file.dto.FileInfoVO;
import io.novafs.file.entity.FileInfo;
import io.novafs.file.entity.FileTransferTask;
import io.novafs.file.enums.TransferTaskStatus;
import io.novafs.file.enums.TransferTaskType;
import io.novafs.file.event.FileUploadCompleteEvent;
import io.novafs.file.mapper.FileChunkInfoMapper;
import io.novafs.file.mapper.FileInfoMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 分片合并服务测试
 */
@ExtendWith(MockitoExtension.class)
class ChunkMergeServiceTest {

    @Mock
    private FileTransferTaskMapper taskMapper;
    @Mock
    private FileChunkInfoMapper chunkInfoMapper;
    @Mock
    private FileInfoMapper fileInfoMapper;
    @Mock
    private ChunkInitService chunkInitService;
    @Mock
    private StorageConfigResolver configResolver;
    @Mock
    private StorageServiceFacade storageFacade;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChunkMergeService service;

    @Test
    void shouldMergeSuccessfully() {
        FileTransferTask task = buildTask();
        task.setUploadId("u1");
        task.setTotalChunks(3);
        task.setFileId(100L);
        when(chunkInitService.requireTaskByUploadId("u1")).thenReturn(task);
        when(chunkInfoMapper.selectCountByQuery(any())).thenReturn(3L);
        when(configResolver.resolve(any())).thenReturn(new StorageConfigResolver.StorageTarget("local", new StorageConfig()));
        when(storageFacade.mergeChunks(eq("local"), any(), any())).thenReturn("task/file.bin");
        when(fileInfoMapper.selectOneById(100L)).thenReturn(buildFile());

        FileInfoVO vo = service.merge(1L, 1L, mergeRequest());

        assertThat(vo).isNotNull();
        verify(taskMapper).update(argThat(t -> t.isCompleted()));
        verify(eventPublisher).publishEvent(any(FileUploadCompleteEvent.class));
    }

    @Test
    void shouldFailWhenChunksIncomplete() {
        FileTransferTask task = buildTask();
        task.setUploadId("u1");
        task.setTotalChunks(3);
        when(chunkInitService.requireTaskByUploadId("u1")).thenReturn(task);
        when(chunkInfoMapper.selectCountByQuery(any())).thenReturn(2L);

        assertThatThrownBy(() -> service.merge(1L, 1L, mergeRequest()))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getCode())
                .isEqualTo(ErrorCode.CHUNK_NOT_COMPLETE.getCode());
    }

    @Test
    void shouldFailWhenTaskAlreadyCompleted() {
        FileTransferTask task = buildTask();
        task.setUploadId("u1");
        task.markCompleted();
        when(chunkInitService.requireTaskByUploadId("u1")).thenReturn(task);

        assertThatThrownBy(() -> service.merge(1L, 1L, mergeRequest()))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getCode())
                .isEqualTo(ErrorCode.CONFLICT.getCode());
    }

    private static FileTransferTask buildTask() {
        FileTransferTask task = new FileTransferTask();
        task.setTaskId("task-1");
        task.setUserId(1L);
        task.setWorkspaceId(1L);
        task.setFileName("big.bin");
        task.setFileSize(1024L);
        task.setStatus(TransferTaskStatus.UPLOADING);
        task.setTaskType(TransferTaskType.UPLOAD);
        task.setStoragePlatformSettingId(1L);
        return task;
    }

    private static FileInfo buildFile() {
        FileInfo file = new FileInfo();
        file.setId(100L);
        file.setWorkspaceId(1L);
        file.setUserId(1L);
        file.setOriginalName("big.bin");
        file.setSize(1024L);
        file.setIsDir(false);
        file.setIsDeleted(false);
        return file;
    }

    private static ChunkMergeRequest mergeRequest() {
        ChunkMergeRequest request = new ChunkMergeRequest();
        request.setUploadId("u1");
        request.setFileName("big.bin");
        return request;
    }
}
