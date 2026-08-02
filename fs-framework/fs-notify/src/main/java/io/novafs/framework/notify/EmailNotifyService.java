package io.novafs.framework.notify;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 邮件通知服务
 * 未启用或未配置 SMTP 时静默跳过，不影响主流程
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotifyService {

    private final NotifyProperties properties;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public void sendMail(String to, String subject, String content) {
        if (!properties.isEnabled()) {
            log.debug("邮件通知未启用，跳过发送: to={}", to);
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("JavaMailSender 未配置（缺少 spring.mail.host），跳过邮件发送");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.getMail().getFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("邮件已发送: to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("邮件发送失败: to={}, subject={}", to, subject, e);
        }
    }
}
