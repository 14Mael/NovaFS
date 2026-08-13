package io.novafs.file.service.impl;

import io.novafs.file.dto.FileInfoVO;
import io.novafs.file.entity.FileInfo;
import io.novafs.file.mapper.FileInfoMapper;
import io.novafs.file.storage.StorageConfigResolver;
import io.novafs.framework.common.model.PageQuery;
import io.novafs.framework.common.model.PageResult;
import com.mybatisflex.core.query.QueryWrapper;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
    void shouldSearchFileByName() {
        FileInfo file = new FileInfo();
        file.setId(100L);
        file.setWorkspaceId(10L);
        file.setOriginalName("年度报告.pdf");
        com.mybatisflex.core.paginate.Page<FileInfo> page = new com.mybatisflex.core.paginate.Page<>();
        page.setRecords(List.of(file));
        page.setTotalRow(1);
        when(fileInfoMapper.paginate(anyInt(), anyInt(), any(QueryWrapper.class))).thenReturn(page);

        PageQuery query = new PageQuery();
        PageResult<FileInfoVO> result = service.search(10L, "报告", query);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords().get(0).getOriginalName()).isEqualTo("年度报告.pdf");
    }

    @Test
    void shouldReturnEmptyWhenKeywordBlank() {
        PageResult<FileInfoVO> result = service.search(10L, "   ", new PageQuery());
        assertThat(result.getRecords()).isEmpty();
    }

    @Test
    void shouldListRootWithIsNullCondition() {
        // 回归：根目录查询必须带 parent_id IS NULL，否则返回全空间文件
        com.mybatisflex.core.paginate.Page<FileInfo> page = new com.mybatisflex.core.paginate.Page<>();
        page.setRecords(List.of());
        page.setTotalRow(0);
        when(fileInfoMapper.paginate(anyInt(), anyInt(), any(QueryWrapper.class))).thenReturn(page);

        service.list(10L, null, new PageQuery());

        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(fileInfoMapper).paginate(anyInt(), anyInt(), captor.capture());
        assertThat(captor.getValue().toSQL()).contains("IS NULL");
    }

    @Test
    void shouldInstantUploadReuseSourceObjectKey() {
        FileInfo source = new FileInfo();
        source.setId(200L);
        source.setWorkspaceId(10L);
        source.setObjectKey("uuid/old.txt");
        source.setStoragePlatformSettingId(50L);
        when(fileInfoMapper.selectOneByQuery(any())).thenReturn(source);
        when(fileInfoMapper.selectCountByQuery(any())).thenReturn(0L);

        FileInfoVO vo = service.instantUpload(1L, 10L, null, "new.txt", "md5hash", 100L);

        ArgumentCaptor<FileInfo> captor = ArgumentCaptor.forClass(FileInfo.class);
        verify(fileInfoMapper).insert(captor.capture());
        assertThat(captor.getValue().getObjectKey()).isEqualTo("uuid/old.txt");
        assertThat(captor.getValue().getStoragePlatformSettingId()).isEqualTo(50L);
        assertThat(captor.getValue().getParentId()).isNull();
        assertThat(captor.getValue().getContentMd5()).isEqualTo("md5hash");
        assertThat(vo.getOriginalName()).isEqualTo("new.txt");
    }

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
