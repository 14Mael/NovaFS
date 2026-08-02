package io.novafs.storage.plugin.boot.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 存储插件注解。
 * 标注在插件实现类上，value 为平台类型标识（如 local、minio）。
 * 同时使实现类成为 Spring Bean，便于 Spring 通道注册。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface StoragePlugin {

    /** 平台类型标识，与 storage_platform.identifier 一致 */
    String value();
}
