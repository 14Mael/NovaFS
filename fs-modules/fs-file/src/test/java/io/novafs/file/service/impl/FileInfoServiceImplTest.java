package io.novafs.file.service.impl;

import io.novafs.file.dto.FileInfoVO;
import io.novafs.file.entity.FileInfo;
import io.novafs.file.mapper.FileInfoMapper;
import io.novafs.file.storage.StorageConfigResolver;
import io.novafs.storage.plugin.boot.StorageServiceFacade;
import io.novafs.storage.plugin.core.model.FileUploadRequest;
import io.novafs.storage.plugin.core.model.StorageConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文件信息服务测试
 */
@ExtendWith(MockitoExtension.class)
class FileInfoServiceImplTest {

    @Mock
    private FileInfoMapper fileInfoMapper;
    @Mock
    private StorageConfigResolver configResolver;
    @Mock
    private StorageServiceFacade storageFacade;

    @InjectMocks
    private FileInfoServiceImpl service;

    @Test
    void shouldRecordRealFileSizeAndMd5OnUpload() throws IOException {
        when(configResolver.resolve(any())).thenReturn(
                new StorageConfigResolver.StorageTarget("local", new StorageConfig()));
        // 模拟本地插件消费输入流（真实插件通过 Files.copy 读完全部字节）
        when(storageFacade.uploadFile(any(), any(), any())).thenAnswer(inv -> {
            FileUploadRequest req = inv.getArgument(2);
            req.getInputStream().transferTo(OutputStream.nullOutputStream());
            return null;
        });

        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        FileInfoVO vo = service.upload(1L, 1L, null, 99L, "hello.txt",
                new ByteArrayInputStream(content));

        ArgumentCaptor<FileInfo> captor = ArgumentCaptor.forClass(FileInfo.class);
        verify(fileInfoMapper).insert(captor.capture());
        FileInfo saved = captor.getValue();
        assertThat(saved.getSize()).isEqualTo(5L);
        assertThat(saved.getContentMd5()).isEqualTo("5d41402abc4b2a76b9719d911017c592");
        assertThat(saved.getObjectKey()).isNotBlank();
        assertThat(vo.getSize()).isEqualTo(5L);
    }

    @Test
    void shouldRecordZeroSizeForEmptyFile() throws IOException {
        when(configResolver.resolve(any())).thenReturn(
                new StorageConfigResolver.StorageTarget("local", new StorageConfig()));
        when(storageFacade.uploadFile(any(), any(), any())).thenAnswer(inv -> {
            FileUploadRequest req = inv.getArgument(2);
            req.getInputStream().transferTo(OutputStream.nullOutputStream());
            return null;
        });

        FileInfoVO vo = service.upload(1L, 1L, null, 99L, "empty.txt",
                new ByteArrayInputStream(new byte[0]));

        ArgumentCaptor<FileInfo> captor = ArgumentCaptor.forClass(FileInfo.class);
        verify(fileInfoMapper).insert(captor.capture());
        assertThat(captor.getValue().getSize()).isZero();
        assertThat(vo.getSize()).isZero();
    }
}
