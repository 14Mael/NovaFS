package io.novafs.file.controller;

import cn.dev33.satoken.stp.StpUtil;
import io.novafs.file.dto.CheckMd5Request;
import io.novafs.file.dto.CheckMd5Result;
import io.novafs.file.dto.ChunkInitRequest;
import io.novafs.file.dto.ChunkInitResponse;
import io.novafs.file.dto.ChunkMergeRequest;
import io.novafs.file.dto.ChunkUploadResponse;
import io.novafs.file.dto.FileInfoVO;
import io.novafs.file.dto.FolderNameRequest;
import io.novafs.file.dto.UploadedChunksResponse;
import io.novafs.file.service.ChunkUploadOrchestrator;
import io.novafs.file.service.FileFolderService;
import io.novafs.file.service.FileInfoService;
import io.novafs.framework.common.model.PageQuery;
import io.novafs.framework.common.model.PageResult;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件控制器：秒传/上传/下载/列表/回收站/分片上传
 */
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final ChunkUploadOrchestrator orchestrator;
    private final FileInfoService fileInfoService;
    private final FileFolderService fileFolderService;
    private final SysUserService sysUserService;

    // ===== 秒传与分片上传 =====

    /** 秒传校验 */
    @PostMapping("/check-md5")
    public Result<CheckMd5Result> checkMd5(@Valid @RequestBody CheckMd5Request request,
                                           @RequestParam Long workspaceId) {
        return Result.ok(orchestrator.checkMd5(request, workspaceId));
    }

    /** 初始化分片上传 */
    @PostMapping("/chunk/init")
    public Result<ChunkInitResponse> chunkInit(@Valid @RequestBody ChunkInitRequest request,
                                               @RequestParam Long workspaceId) {
        return Result.ok(orchestrator.initChunkUpload(currentUserId(), workspaceId, request));
    }

    /** 上传分片（幂等） */
    @PostMapping("/chunk/upload")
    public Result<ChunkUploadResponse> chunkUpload(@RequestParam String uploadId,
                                                   @RequestParam Integer chunkNumber,
                                                   @RequestParam(required = false) String md5,
                                                   @RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(orchestrator.uploadChunk(uploadId, chunkNumber, md5,
                file.getInputStream(), file.getSize()));
    }

    /** 查询已上传分片（断点续传） */
    @GetMapping("/chunk/list")
    public Result<UploadedChunksResponse> chunkList(@RequestParam String uploadId) {
        return Result.ok(orchestrator.listUploadedChunks(uploadId));
    }

    /** 合并分片 */
    @PostMapping("/chunk/merge")
    public Result<FileInfoVO> chunkMerge(@Valid @RequestBody ChunkMergeRequest request,
                                         @RequestParam Long workspaceId) {
        return Result.ok(orchestrator.mergeChunks(currentUserId(), workspaceId, request));
    }

    // ===== 文件基础操作 =====

    /** 普通上传（小文件） */
    @PostMapping("/upload")
    public Result<FileInfoVO> upload(@RequestParam Long workspaceId,
                                     @RequestParam(required = false) Long parentId,
                                     @RequestParam Long storagePlatformSettingId,
                                     @RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(fileInfoService.upload(currentUserId(), workspaceId, parentId,
                storagePlatformSettingId, file.getOriginalFilename(), file.getInputStream()));
    }

    /** 文件列表 */
    @GetMapping("/list")
    public Result<PageResult<FileInfoVO>> list(@RequestParam Long workspaceId,
                                               @RequestParam(required = false) Long parentId,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int pageSize) {
        PageQuery query = new PageQuery();
        query.setPage(page);
        query.setPageSize(pageSize);
        return Result.ok(fileInfoService.list(workspaceId, parentId, query));
    }

    /** 按文件名搜索（工作空间范围内） */
    @GetMapping("/search")
    public Result<PageResult<FileInfoVO>> search(@RequestParam Long workspaceId,
                                                 @RequestParam String keyword,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int pageSize) {
        PageQuery query = new PageQuery();
        query.setPage(page);
        query.setPageSize(pageSize);
        return Result.ok(fileInfoService.search(workspaceId, keyword, query));
    }

    /** 下载 */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long fileId) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment")
                .body(new InputStreamResource(fileInfoService.download(fileId, currentUserId())));
    }

    /** 删除（软删除进回收站） */
    @DeleteMapping("/{fileId}")
    public Result<Void> delete(@PathVariable Long fileId) {
        fileInfoService.delete(fileId, currentUserId());
        return Result.ok();
    }

    /** 创建文件夹 */
    @PostMapping("/folder")
    public Result<FileInfoVO> createFolder(@RequestParam Long workspaceId,
                                           @RequestParam(required = false) Long parentId,
                                           @Valid @RequestBody FolderNameRequest request) {
        return Result.ok(fileFolderService.createFolder(currentUserId(), workspaceId, parentId, request.getName()));
    }

    /** 重命名（文件或文件夹） */
    @PutMapping("/{fileId}")
    public Result<FileInfoVO> rename(@PathVariable Long fileId,
                                     @Valid @RequestBody FolderNameRequest request) {
        return Result.ok(fileFolderService.rename(currentUserId(), fileId, request.getName()));
    }

    /** 移动（parentId 为空表示移动到根目录） */
    @PutMapping("/{fileId}/move")
    public Result<FileInfoVO> move(@PathVariable Long fileId,
                                   @RequestParam(required = false) Long parentId) {
        return Result.ok(fileFolderService.move(currentUserId(), fileId, parentId));
    }

    /** 回收站列表 */
    @GetMapping("/recycle")
    public Result<PageResult<FileInfoVO>> recycle(@RequestParam Long workspaceId,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int pageSize) {
        PageQuery query = new PageQuery();
        query.setPage(page);
        query.setPageSize(pageSize);
        return Result.ok(fileInfoService.recycleList(workspaceId, query));
    }

    /** 从回收站恢复 */
    @PostMapping("/{fileId}/restore")
    public Result<Void> restore(@PathVariable Long fileId) {
        fileInfoService.restore(fileId, currentUserId());
        return Result.ok();
    }

    /** 彻底删除 */
    @DeleteMapping("/{fileId}/purge")
    public Result<Void> purge(@PathVariable Long fileId) {
        fileInfoService.purge(fileId, currentUserId());
        return Result.ok();
    }

    private Long currentUserId() {
        return sysUserService.findByUsername(StpUtil.getLoginIdAsString()).getId();
    }
}
