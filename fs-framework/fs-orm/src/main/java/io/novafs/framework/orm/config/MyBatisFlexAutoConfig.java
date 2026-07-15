package io.novafs.framework.orm.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Flex 全局配置
 */
@Configuration
public class MyBatisFlexAutoConfig implements MyBatisFlexCustomizer {

    @Override
    public void customize(FlexGlobalConfig globalConfig) {
        globalConfig.setLogicDeleteColumn("is_deleted");
    }
}
