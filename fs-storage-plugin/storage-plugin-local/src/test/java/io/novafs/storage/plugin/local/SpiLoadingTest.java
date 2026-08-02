package io.novafs.storage.plugin.local;

import io.novafs.storage.plugin.core.IStorageOperationService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SPI 加载测试：验证本地插件通过 META-INF/services 可被 ServiceLoader 加载
 */
class SpiLoadingTest {

    @Test
    void shouldLoadLocalPluginViaSpi() {
        List<IStorageOperationService> plugins = new ArrayList<>();
        ServiceLoader.load(IStorageOperationService.class).forEach(plugins::add);

        assertThat(plugins)
                .anyMatch(p -> "local".equals(p.getPlatformType()));
    }
}
