package io.novafs.rag.service;

import io.novafs.rag.vo.ChatResponseVO;

/**
 * RAG 问答服务
 */
public interface RagChatService {

    /** 基于工作空间内文档回答问题 */
    ChatResponseVO chat(Long workspaceId, String question);
}