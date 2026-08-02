package io.novafs.framework.orm.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Flex 全局配置
 * <p>注意：不启用全局逻辑删除（setLogicDeleteColumn），
 * 项目中 file_info / storage_settings 的 is_deleted 由各 Service 手动维护，
 * 避免框架自动追加条件与手动条件冲突（重复条件、回收站查不到、deleteById 变软删）。</p>
 */
@Configuration
public class MyBatisFlexAutoConfig implements MyBatisFlexCustomizer {

    @Override
    public void customize(FlexGlobalConfig globalConfig) {
        // 逻辑删除由各 Service 手动控制
    }
}
