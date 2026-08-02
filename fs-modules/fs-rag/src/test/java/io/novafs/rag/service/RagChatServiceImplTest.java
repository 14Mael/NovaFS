package io.novafs.rag.service;

import io.novafs.rag.client.ChatClient;
import io.novafs.rag.service.impl.RagChatServiceImpl;
import io.novafs.rag.vo.ChatResponseVO;
import io.novafs.rag.vo.SearchResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagChatServiceImplTest {

    @Mock
    private RagSearchService searchService;
    @Mock
    private ChatClient chatClient;

    private RagChatServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RagChatServiceImpl(searchService, chatClient);
    }

    @Test
    void chat_injectsContextIntoPrompt() {
        SearchResultVO vo = new SearchResultVO();
        vo.setDocumentName("a.txt");
        vo.setContent("这是文档关键内容");
        when(searchService.search(1L, "问题", null)).thenReturn(List.of(vo));
        when(chatClient.chat(anyString(), eq("问题"))).thenReturn("根据文档的回答");

        ChatResponseVO resp = service.chat(1L, "问题");

        assertEquals("根据文档的回答", resp.getAnswer());
        assertEquals(1, resp.getSources().size());
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(chatClient).chat(captor.capture(), eq("问题"));
        assertTrue(captor.getValue().contains("这是文档关键内容"));
        assertTrue(captor.getValue().contains("a.txt"));
    }
}