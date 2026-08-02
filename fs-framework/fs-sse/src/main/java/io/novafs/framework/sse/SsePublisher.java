package io.novafs.framework.sse;

import io.novafs.framework.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * SSE 消息发布器：通过 Redis Pub/Sub 广播，多实例部署时所有实例都能收到
 */
@Component
@RequiredArgsConstructor
public class SsePublisher {

    public static final String CHANNEL = "novafs:sse";

    private final StringRedisTemplate redisTemplate;

    public void publish(Long userId, String eventName, Object data) {
        SseMessage message = new SseMessage(userId, eventName,
                data == null ? null : JsonUtils.toJson(data));
        redisTemplate.convertAndSend(CHANNEL, JsonUtils.toJson(message));
    }
}
