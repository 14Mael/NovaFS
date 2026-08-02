package io.novafs.storage.plugin.boot;

import io.novafs.storage.plugin.core.IStorageOperationService;
import io.novafs.storage.plugin.core.exception.UnsupportedStoragePlatformException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储插件注册中心。
 * 双通道加载：Java SPI（ServiceLoader）+ Spring Bean，按平台类型索引。
 */
@Slf4j
@Component
public class StoragePluginRegistry implements ApplicationContextAware {

    private final Map<String, IStorageOperationService> pluginMap = new ConcurrentHashMap<>();

    private ApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        registerSpiPlugins();
        registerSpringPlugins();
        log.info("存储插件注册完成，共 {} 个: {}", pluginMap.size(), pluginMap.keySet());
    }

    /** 通过 Java SPI 加载插件 */
    private void registerSpiPlugins() {
        for (IStorageOperationService plugin : ServiceLoader.load(IStorageOperationService.class)) {
            pluginMap.putIfAbsent(plugin.getPlatformType(), plugin);
            log.debug("SPI 注册存储插件: {}", plugin.getPlatformType());
        }
    }

    /** 通过 Spring Bean 注册插件（插件标注 @StoragePlugin 即自动成为 Bean） */
    private void registerSpringPlugins() {
        applicationContext.getBeansOfType(IStorageOperationService.class)
                .forEach((beanName, plugin) -> {
                    pluginMap.putIfAbsent(plugin.getPlatformType(), plugin);
                    log.debug("Spring 注册存储插件: {}", plugin.getPlatformType());
                });
    }

    /** 按平台类型获取插件 */
    public IStorageOperationService getPlugin(String platformType) {
        IStorageOperationService plugin = pluginMap.get(platformType);
        if (plugin == null) {
            throw new UnsupportedStoragePlatformException(platformType);
        }
        return plugin;
    }

    /** 已注册的平台类型列表 */
    public List<String> getSupportedPlatformTypes() {
        return new ArrayList<>(pluginMap.keySet());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
