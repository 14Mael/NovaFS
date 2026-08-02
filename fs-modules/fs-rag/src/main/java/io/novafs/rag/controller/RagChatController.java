package io.novafs.rag.controller;

import io.novafs.framework.common.model.Result;
import io.novafs.rag.dto.ChatRequest;
import io.novafs.rag.dto.SearchRequest;
import io.novafs.rag.service.RagChatService;
import io.novafs.rag.service.RagSearchService;
import io.novafs.rag.vo.ChatResponseVO;
import io.novafs.rag.vo.SearchResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * RAG 检索与问答接口
 */
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagChatController {

    private final RagSearchService searchService;
    private final RagChatService chatService;

    /** 语义检索 */
    @PostMapping("/search")
    public Result<List<SearchResultVO>> search(@Valid @RequestBody SearchRequest request) {
        return Result.ok(searchService.search(request.getWorkspaceId(), request.getQuery(), request.getTopK()));
    }

    /** 基于文档问答 */
    @PostMapping("/chat")
    public Result<ChatResponseVO> chat(@Valid @RequestBody ChatRequest request) {
        return Result.ok(chatService.chat(request.getWorkspaceId(), request.getQuestion()));
    }
}