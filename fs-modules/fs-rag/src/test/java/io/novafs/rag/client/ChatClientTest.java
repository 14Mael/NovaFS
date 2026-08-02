package io.novafs.rag.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ChatClientTest {

    private ChatClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ChatClient(builder, "http://localhost:8080", "test-key", "gpt-4o-mini", 0.3, 1024);
    }

    @Test
    void chat_returnsContent() {
        server.expect(requestTo("http://localhost:8080/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":"你好,这是回答"}}]}
                        """, MediaType.APPLICATION_JSON));

        String answer = client.chat("你是助手", "你好");

        assertEquals("你好,这是回答", answer);
        server.verify();
    }

    @Test
    void chat_emptyChoices_throws() {
        server.expect(requestTo("http://localhost:8080/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));
        assertThrows(io.novafs.framework.common.exception.BaseException.class,
                () -> client.chat("sys", "q"));
    }
}