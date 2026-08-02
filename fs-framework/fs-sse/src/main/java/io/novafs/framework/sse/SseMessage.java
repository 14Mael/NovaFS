package io.novafs.framework.sse;

/**
 * SSE 广播消息（经 Redis Pub/Sub 传递）
 */
public record SseMessage(Long userId, String eventName, String data) {
}
