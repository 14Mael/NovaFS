package io.novafs.rag.service.impl;

import io.novafs.rag.client.ChatClient;
import io.novafs.rag.service.RagChatService;
import io.novafs.rag.service.RagSearchService;
import io.novafs.rag.vo.ChatResponseVO;
import io.novafs.rag.vo.SearchResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG 问答服务实现
 */
@Service
@RequiredArgsConstructor
public class RagChatServiceImpl implements RagChatService {

    private static final String SYSTEM_PROMPT = """
            你是一个严谨的文档问答助手。请仅根据提供的文档内容回答问题。
            若文档内容不足以回答,请明确说明"根据现有文档无法回答"。
            回答时引用相关来源文档名称,使用简体中文。""";

    private final RagSearchService searchService;
    private final ChatClient chatClient;

    @Override
    public ChatResponseVO chat(Long workspaceId, String question) {
        List<SearchResultVO> sources = searchService.search(workspaceId, question, null);
        String context = buildContext(sources);
        String answer = chatClient.chat(SYSTEM_PROMPT + "\n\n【文档内容】\n" + context, question);
        return new ChatResponseVO(answer, sources);
    }

    private static String buildContext(List<SearchResultVO> sources) {
        StringBuilder sb = new StringBuilder();
        for (SearchResultVO s : sources) {
            sb.append("[来源: ").append(s.getDocumentName()).append("]\n")
                    .append(s.getContent()).append("\n\n");
        }
        return sb.toString();
    }
}