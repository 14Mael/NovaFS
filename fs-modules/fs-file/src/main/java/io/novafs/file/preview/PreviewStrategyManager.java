package io.novafs.file.preview;

import io.novafs.file.entity.FileInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 预览策略管理器：按优先级匹配策略
 */
@Component
@RequiredArgsConstructor
public class PreviewStrategyManager {

    private final List<PreviewStrategy> strategies;

    public PreviewStrategy match(FileInfo file) {
        return strategies.stream()
                .filter(s -> s.support(file))
                .min(Comparator.comparingInt(PreviewStrategy::getOrder))
                .orElseThrow(() -> new IllegalStateException("无可用预览策略"));
    }
}
