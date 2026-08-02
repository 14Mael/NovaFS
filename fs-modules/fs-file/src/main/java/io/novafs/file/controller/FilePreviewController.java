package io.novafs.file.controller;

import cn.dev33.satoken.stp.StpUtil;
import io.novafs.file.dto.FilePreviewVO;
import io.novafs.file.entity.FileInfo;
import io.novafs.file.mapper.FileInfoMapper;
import io.novafs.file.service.FileInfoService;
import io.novafs.file.service.PreviewService;
import io.novafs.framework.common.model.Result;
import io.novafs.system.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件预览控制器
 */
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FilePreviewController {

    private final PreviewService previewService;
    private final FileInfoService fileInfoService;
    private final FileInfoMapper fileInfoMapper;
    private final SysUserService sysUserService;

    /** 预览信息（类型 + 地址/内容） */
    @GetMapping("/preview/{fileId}")
    public Result<FilePreviewVO> preview(@PathVariable Long fileId) {
        return Result.ok(previewService.getPreviewInfo(fileId, currentUserId()));
    }

    /** 预览内容流（图片/音视频/PDF 用，带鉴权） */
    @GetMapping("/preview/content/{fileId}")
    public ResponseEntity<InputStreamResource> content(@PathVariable Long fileId) {
        Long userId = currentUserId();
        FileInfo file = fileInfoMapper.selectOneById(fileId);
        MediaType mediaType = file != null && file.getMimeType() != null
                ? MediaType.parseMediaType(file.getMimeType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(new InputStreamResource(fileInfoService.download(fileId, userId)));
    }

    private Long currentUserId() {
        return sysUserService.findByUsername(StpUtil.getLoginIdAsString()).getId();
    }
}
