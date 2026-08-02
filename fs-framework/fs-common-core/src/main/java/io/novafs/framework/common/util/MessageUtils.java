package io.novafs.framework.common.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * 国际化消息工具
 */
public final class MessageUtils {

    private MessageUtils() {
    }

    /**
     * 获取国际化消息，未找到时返回默认消息
     */
    public static String getMessage(String code, String defaultMessage, Object... args) {
        try {
            MessageSource messageSource = SpringUtils.getBean(MessageSource.class);
            if (messageSource != null) {
                return messageSource.getMessage(code, args, defaultMessage, LocaleContextHolder.getLocale());
            }
        } catch (Exception ignored) {
            // MessageSource 未就绪时回退默认消息
        }
        return defaultMessage;
    }
}
