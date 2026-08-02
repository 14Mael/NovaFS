package io.novafs.framework.notify;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 通知配置
 * SMTP 参数使用标准 spring.mail.* 配置，本配置仅控制开关与发件人
 */
@Data
@Component
@ConfigurationProperties(prefix = "novafs.notify")
public class NotifyProperties {

    /** 是否启用通知（未启用时静默跳过） */
    private boolean enabled = false;

    private Mail mail = new Mail();

    @Data
    public static class Mail {

        /** 发件人地址 */
        private String from;
    }
}
