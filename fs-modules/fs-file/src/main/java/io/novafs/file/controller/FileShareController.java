package io.novafs.file.controller;

import cn.dev33.satoken.stp.StpUtil;
import io.novafs.file.dto.FileShareRequest;
import io.novafs.file.dto.FileShareVO;
import io.novafs.file.service.FileShareService;
import io.novafs.framework.common.model.Result;
import io.novafs.system.user.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件分享控制器
 * 管理接口走 /api/shares（需登录），公开访问走 /api/share/{code}（免登录，SaTokenConfig 白名单）
 */
@RestController
@RequiredArgsConstructor
public class FileShareController {

    private final FileShareService shareService;
    private final SysUserService sysUserService;

    /** 创建分享（需登录） */
    @PostMapping("/api/shares")
    public Result<FileShareVO> create(@RequestBody @Valid FileShareRequest request) {
        return Result.ok(shareService.create(currentUserId(), request));
    }

    /** 通过分享码访问（公开） */
    @GetMapping("/api/share/{shareCode}")
    public Result<FileShareVO> access(@PathVariable String shareCode,
                                      @RequestParam(required = false) String password) {
        return Result.ok(shareService.access(shareCode, password));
    }

    /** 通过分享码下载（公开，带提取码/过期/次数校验） */
    @GetMapping("/api/share/{shareCode}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable String shareCode,
                                                        @RequestParam(required = false) String password,
                                                        @RequestParam(defaultValue = "false") boolean inline) {
        FileShareService.StreamDownload result = shareService.download(shareCode, password);
        FileShareVO vo = result.share();
        String filename = URLEncoder.encode(vo.getFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        String disposition = (inline ? "inline" : "attachment")
                + "; filename*=UTF-8''" + filename;
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", disposition)
                .body(new InputStreamResource(result.in()));
    }

    /** 取消分享（需登录） */
    @DeleteMapping("/api/shares/{shareId}")
    public Result<Void> cancel(@PathVariable Long shareId) {
        shareService.cancel(shareId, currentUserId());
        return Result.ok();
    }

    private Long currentUserId() {
        return sysUserService.findByUsername(StpUtil.getLoginIdAsString()).getId();
    }
}
