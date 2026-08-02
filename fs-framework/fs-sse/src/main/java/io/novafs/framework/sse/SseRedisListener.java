package io.novafs.framework.sse;

import io.novafs.framework.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * SSE Redis 消息监听器：收到广播后推送给本实例上的连接
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseRedisListener implements MessageListener {

    private final SseConnectionManager connectionManager;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            SseMessage sseMessage = JsonUtils.parse(json, SseMessage.class);
            if (sseMessage == null) {
                return;
            }
            connectionManager.sendLocal(sseMessage.userId(), sseMessage.eventName(), sseMessage.data());
        } catch (Exception e) {
            log.warn("处理 SSE 广播消息失败", e);
        }
    }
}
