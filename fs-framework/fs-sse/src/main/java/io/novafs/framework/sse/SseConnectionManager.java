package io.novafs.framework.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE 连接管理器：维护 userId → SseEmitter 连接，支持多连接
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseConnectionManager {

    private final SsePublisher ssePublisher;

    private final Map<Long, List<SseEmitter>> connections = new ConcurrentHashMap<>();

    /**
     * 建立连接
     */
    public SseEmitter connect(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));
        connections.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        return emitter;
    }

    /**
     * 发送消息（经 Redis 广播，所有实例的该用户连接都能收到）
     */
    public void send(Long userId, String eventName, Object data) {
        ssePublisher.publish(userId, eventName, data);
    }

    /**
     * 推送给本实例上的连接（由 Redis 监听器回调）
     */
    public void sendLocal(Long userId, String eventName, Object data) {
        List<SseEmitter> emitters = connections.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException | IllegalStateException e) {
                log.debug("SSE 推送失败，移除连接: userId={}", userId);
                remove(userId, emitter);
            }
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = connections.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                connections.remove(userId);
            }
        }
    }
}
