package io.novafs.file.service.impl;

import io.novafs.file.dto.FileShareRequest;
import io.novafs.file.dto.FileShareVO;
import io.novafs.file.entity.FileInfo;
import io.novafs.file.entity.FileShare;
import io.novafs.file.mapper.FileInfoMapper;
import io.novafs.file.mapper.FileShareMapper;
import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 文件分享服务测试
 */
@ExtendWith(MockitoExtension.class)
class FileShareServiceImplTest {

    @Mock
    private FileShareMapper shareMapper;
    @Mock
    private FileInfoMapper fileInfoMapper;

    private FileShareServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FileShareServiceImpl(shareMapper, fileInfoMapper);
    }

    @Test
    void shouldCreateShare() {
        when(fileInfoMapper.selectOneById(100L)).thenReturn(ownedFile());
        when(shareMapper.selectCountByQuery(any())).thenReturn(0L);

        FileShareRequest request = new FileShareRequest();
        request.setFileId(100L);
        request.setSharePwd("1234");
        FileShareVO vo = service.create(1L, request);

        assertThat(vo.getShareCode()).hasSize(6);
        assertThat(vo.isHasPassword()).isTrue();
        ArgumentCaptor<FileShare> captor = ArgumentCaptor.forClass(FileShare.class);
        org.mockito.Mockito.verify(shareMapper).insert(captor.capture());
        assertThat(new BCryptPasswordEncoder().matches("1234", captor.getValue().getSharePwd())).isTrue();
    }

    @Test
    void shouldRejectShareForbidden() {
        when(fileInfoMapper.selectOneById(100L)).thenReturn(ownedFile());

        FileShareRequest request = new FileShareRequest();
        request.setFileId(100L);

        assertThatThrownBy(() -> service.create(2L, request))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getCode())
                .isEqualTo(ErrorCode.FORBIDDEN.getCode());
    }

    @Test
    void shouldAccessWithCorrectPassword() {
        when(shareMapper.selectOneByQuery(any())).thenReturn(shareWithPassword());
        when(fileInfoMapper.selectOneById(100L)).thenReturn(ownedFile());

        FileShareVO vo = service.access("ABCDEF", "1234");

        assertThat(vo.getShareCode()).isEqualTo("ABCDEF");
        assertThat(vo.getFileName()).isEqualTo("doc.pdf");
    }

    @Test
    void shouldRejectWrongPassword() {
        when(shareMapper.selectOneByQuery(any())).thenReturn(shareWithPassword());

        assertThatThrownBy(() -> service.access("ABCDEF", "wrong"))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getCode())
                .isEqualTo(ErrorCode.SHARE_PASSWORD_ERROR.getCode());
    }

    @Test
    void shouldRejectExpiredShare() {
        FileShare share = shareWithPassword();
        share.setExpireTime(LocalDateTime.now().minusDays(1));
        when(shareMapper.selectOneByQuery(any())).thenReturn(share);

        assertThatThrownBy(() -> service.access("ABCDEF", "1234"))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getCode())
                .isEqualTo(ErrorCode.SHARE_EXPIRED.getCode());
    }

    private static FileInfo ownedFile() {
        FileInfo file = new FileInfo();
        file.setId(100L);
        file.setUserId(1L);
        file.setWorkspaceId(1L);
        file.setOriginalName("doc.pdf");
        file.setSuffix("pdf");
        file.setSize(1024L);
        return file;
    }

    private static FileShare shareWithPassword() {
        FileShare share = new FileShare();
        share.setId(1L);
        share.setUserId(1L);
        share.setWorkspaceId(1L);
        share.setFileId(100L);
        share.setShareCode("ABCDEF");
        share.setSharePwd(new BCryptPasswordEncoder().encode("1234"));
        share.setViewCount(0);
        share.setDownloadCount(0);
        share.setScope("PREVIEW,DOWNLOAD");
        return share;
    }
}
