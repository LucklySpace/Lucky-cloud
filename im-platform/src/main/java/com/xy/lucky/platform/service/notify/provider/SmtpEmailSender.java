package com.xy.lucky.platform.service.notify.provider;

import com.xy.lucky.platform.config.EmailProperties;
import com.xy.lucky.platform.domain.vo.EmailVo;
import com.xy.lucky.platform.service.notify.EmailSender;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.Properties;

/**
 * 邮件发送
 */
@Slf4j
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final EmailProperties.Smtp smtp;

    /**
     * 发送邮件
     *
     * @param email 邮件参数
     * @return 邮件发送结果
     */
    @Override
    public Boolean send(EmailVo email) {

        JavaMailSender sender = setMailInfo();

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(smtp.getFrom());
            helper.setTo(email.getTo());
            helper.setSubject(email.getSubject());
            helper.setText(email.getContent(), Boolean.TRUE.equals(email.getHtml()));
            sender.send(message);
            log.info("邮件已发送至 {}", email.getTo());
            return true;
        } catch (Exception e) {
            log.error("邮件发送失败: {}", e.getMessage(), e);
            return false;
        }
    }


    /**
     * 设置邮件信息
     *
     * @return 邮件发送者
     */
    private JavaMailSender setMailInfo() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(smtp.getHost());
        if (smtp.getPort() != null) sender.setPort(smtp.getPort());
        sender.setUsername(smtp.getUsername());
        sender.setPassword(smtp.getPassword());
        sender.setDefaultEncoding("UTF-8");
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(!smtp.isSsl()));
        props.put("mail.smtp.ssl.enable", String.valueOf(smtp.isSsl()));
        sender.setJavaMailProperties(props);
        return sender;
    }
}
