package io.novafs.framework.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * SSE Redis 订阅配置
 */
@Configuration
@RequiredArgsConstructor
public class SseRedisConfig {

    private final SseRedisListener listener;

    @Bean
    public RedisMessageListenerContainer sseListenerContainer(RedisConnectionFactory factory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(listener, new ChannelTopic(SsePublisher.CHANNEL));
        return container;
    }
}
