package io.novafs.file.service.impl;

import io.novafs.file.dto.FileInfoVO;
import io.novafs.file.entity.FileInfo;
import io.novafs.file.mapper.FileInfoMapper;
import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文件夹管理服务测试：创建/重命名/移动（重名、防环、归属）
 */
@ExtendWith(MockitoExtension.class)
class FileFolderServiceImplTest {

    @Mock
    private FileInfoMapper fileInfoMapper;

    private FileFolderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FileFolderServiceImpl(fileInfoMapper);
    }

    @Test
    void shouldCreateFolder() {
        when(fileInfoMapper.selectCountByQuery(any())).thenReturn(0L);

        FileInfoVO vo = service.createFolder(1L, 10L, null, "项目资料");

        ArgumentCaptor<FileInfo> captor = ArgumentCaptor.forClass(FileInfo.class);
        verify(fileInfoMapper).insert(captor.capture());
        FileInfo inserted = captor.getValue();
        assertThat(inserted.getIsDir()).isTrue();
        assertThat(inserted.getOriginalName()).isEqualTo("项目资料");
        assertThat(inserted.getWorkspaceId()).isEqualTo(10L);
        assertThat(inserted.getUserId()).isEqualTo(1L);
        assertThat(inserted.getParentId()).isNull();
        assertThat(vo.getIsDir()).isTrue();
    }

    @Test
    void shouldRejectDuplicateNameInSameFolder() {
        when(fileInfoMapper.selectCountByQuery(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.createFolder(1L, 10L, null, "项目资料"))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getCode())
                .isEqualTo(ErrorCode.BAD_REQUEST.getCode());
    }

    @Test
    void shouldRejectInvalidNameWithPathSeparator() {
        assertThatThrownBy(() -> service.createFolder(1L, 10L, null, "a/b"))
                .isInstanceOf(BaseException.class);
        verify(fileInfoMapper, never()).insert(any());
    }

    @Test
    void shouldRenameFile() {
        FileInfo file = file(100L, 10L, null, "old.txt", false);
        when(fileInfoMapper.selectOneById(100L)).thenReturn(file);
        when(fileInfoMapper.selectCountByQuery(any())).thenReturn(0L);

        service.rename(1L, 100L, "new.txt");

        assertThat(file.getOriginalName()).isEqualTo("new.txt");
        verify(fileInfoMapper).update(file);
    }

    @Test
    void shouldRejectRenameToExistingName() {
        FileInfo file = file(100L, 10L, null, "old.txt", false);
        when(fileInfoMapper.selectOneById(100L)).thenReturn(file);
        when(fileInfoMapper.selectCountByQuery(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.rename(1L, 100L, "dup.txt"))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getCode())
                .isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        verify(fileInfoMapper, never()).updateParentId(any(), any());
    }

    @Test
    void shouldRejectMoveIntoOwnDescendant() {
        FileInfo dir = dir(100L, 10L, null, "root");
        FileInfo child = dir(200L, 10L, 100L, "child");
        when(fileInfoMapper.selectOneById(100L)).thenReturn(dir);
        when(fileInfoMapper.selectOneById(200L)).thenReturn(child);

        assertThatThrownBy(() -> service.move(1L, 100L, 200L))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getCode())
                .isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        verify(fileInfoMapper, never()).updateParentId(any(), any());
    }

    @Test
    void shouldMoveToRoot() {
        FileInfo file = file(100L, 10L, 5L, "a.txt", false);
        when(fileInfoMapper.selectOneById(100L)).thenReturn(file);
        when(fileInfoMapper.selectCountByQuery(any())).thenReturn(0L);

        service.move(1L, 100L, null);

        assertThat(file.getParentId()).isNull();
        verify(fileInfoMapper).updateParentId(eq(100L), isNull());
    }

    @Test
    void shouldNotDeadLoopOnCircularData() {
        // 脏数据成环：200 -> 300 -> 200，防环上溯必须终止而非死循环
        FileInfo dir = dir(100L, 10L, null, "root");
        FileInfo a = dir(200L, 10L, 300L, "a");
        FileInfo b = dir(300L, 10L, 200L, "b");
        when(fileInfoMapper.selectOneById(100L)).thenReturn(dir);
        when(fileInfoMapper.selectOneById(200L)).thenReturn(a);
        when(fileInfoMapper.selectOneById(300L)).thenReturn(b);

        assertThatThrownBy(() -> service.move(1L, 100L, 200L))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getCode())
                .isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        verify(fileInfoMapper, never()).updateParentId(any(), any());
    }

    @Test
    void shouldRejectRenameDeletedFile() {
        FileInfo file = file(100L, 10L, null, "old.txt", false);
        file.setIsDeleted(true);
        when(fileInfoMapper.selectOneById(100L)).thenReturn(file);

        assertThatThrownBy(() -> service.rename(1L, 100L, "new.txt"))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getCode())
                .isEqualTo(ErrorCode.FILE_NOT_FOUND.getCode());
        verify(fileInfoMapper, never()).updateParentId(any(), any());
    }

    @Test
    void shouldRejectTargetFolderFromOtherWorkspace() {
        FileInfo file = file(100L, 10L, null, "a.txt", false);
        FileInfo foreignDir = dir(300L, 99L, null, "foreign");
        when(fileInfoMapper.selectOneById(100L)).thenReturn(file);
        when(fileInfoMapper.selectOneById(300L)).thenReturn(foreignDir);

        assertThatThrownBy(() -> service.move(1L, 100L, 300L))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getCode())
                .isEqualTo(ErrorCode.FORBIDDEN.getCode());
    }

    @Test
    void shouldRejectFileNotOwned() {
        FileInfo file = file(100L, 10L, null, "a.txt", false);
        file.setUserId(9L);
        when(fileInfoMapper.selectOneById(anyLong())).thenReturn(file);

        assertThatThrownBy(() -> service.rename(1L, 100L, "x.txt"))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getCode())
                .isEqualTo(ErrorCode.FORBIDDEN.getCode());
    }

    private static FileInfo file(Long id, Long workspaceId, Long parentId, String name, boolean isDir) {
        FileInfo f = new FileInfo();
        f.setId(id);
        f.setWorkspaceId(workspaceId);
        f.setParentId(parentId);
        f.setOriginalName(name);
        f.setIsDir(isDir);
        f.setIsDeleted(false);
        f.setUserId(1L);
        return f;
    }

    private static FileInfo dir(Long id, Long workspaceId, Long parentId, String name) {
        return file(id, workspaceId, parentId, name, true);
    }
}
