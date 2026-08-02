package io.novafs.rag.controller;

import cn.dev33.satoken.stp.StpUtil;
import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import io.novafs.framework.common.model.PageQuery;
import io.novafs.framework.common.model.PageResult;
import io.novafs.framework.common.model.Result;
import io.novafs.rag.dto.IngestTextRequest;
import io.novafs.rag.service.RagDocumentService;
import io.novafs.rag.vo.ChunkVO;
import io.novafs.rag.vo.DocumentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * RAG 文档管理接口
 */
@RestController
@RequestMapping("/api/rag/documents")
@RequiredArgsConstructor
public class RagDocumentController {

    private final RagDocumentService ragDocumentService;

    /** 上传文件入库 */
    @PostMapping
    public Result<DocumentVO> upload(@RequestParam("workspaceId") Long workspaceId,
                                     @RequestPart("file") MultipartFile file) {
        long userId = StpUtil.getLoginIdAsLong();
        try (InputStream in = file.getInputStream()) {
            DocumentVO vo = ragDocumentService.ingestFile(
                    workspaceId, userId,
                    file.getOriginalFilename(), file.getContentType(), file.getSize(), in);
            return Result.ok(vo);
        } catch (IOException e) {
            throw new BaseException(ErrorCode.RAG_DOCUMENT_PARSE_FAILED, "读取上传文件失败");
        }
    }

    /** 文本直入入库 */
    @PostMapping("/text")
    public Result<DocumentVO> ingestText(@Valid @RequestBody IngestTextRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(ragDocumentService.ingestText(request.getWorkspaceId(), userId, request.getName(), request.getContent()));
    }

    /** 分页查询文档列表 */
    @GetMapping
    public Result<PageResult<DocumentVO>> page(@RequestParam("workspaceId") Long workspaceId,
                                               @Valid PageQuery query) {
        return Result.ok(ragDocumentService.page(workspaceId, query));
    }

    /** 删除文档(同步清理切片与向量) */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        ragDocumentService.delete(id);
        return Result.ok();
    }

    /** 查询文档切片列表 */
    @GetMapping("/{id}/chunks")
    public Result<List<ChunkVO>> chunks(@PathVariable Long id) {
        return Result.ok(ragDocumentService.listChunks(id));
    }
}