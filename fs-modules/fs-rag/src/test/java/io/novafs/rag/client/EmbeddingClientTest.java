package io.novafs.rag.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EmbeddingClientTest {

    private EmbeddingClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new EmbeddingClient(builder, "http://localhost:8080", "test-key", "text-embedding-3-small", 3);
    }

    @Test
    void embedAll_returnsVectorsInOrder() {
        server.expect(requestTo("http://localhost:8080/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andRespond(withSuccess("""
                        {"data":[
                          {"index":0,"embedding":[0.1,0.2,0.3]},
                          {"index":1,"embedding":[0.4,0.5,0.6]}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        List<float[]> result = client.embedAll(List.of("第一段", "第二段"));

        assertEquals(2, result.size());
        assertArrayEquals(new float[]{0.1f, 0.2f, 0.3f}, result.get(0), 1e-6f);
        assertArrayEquals(new float[]{0.4f, 0.5f, 0.6f}, result.get(1), 1e-6f);
        server.verify();
    }

    @Test
    void embed_singleText() {
        server.expect(requestTo("http://localhost:8080/embeddings"))
                .andRespond(withSuccess("{\"data\":[{\"index\":0,\"embedding\":[1.0,2.0,3.0]}]}", MediaType.APPLICATION_JSON));
        float[] vector = client.embed("hello");
        assertArrayEquals(new float[]{1.0f, 2.0f, 3.0f}, vector, 1e-6f);
    }
}