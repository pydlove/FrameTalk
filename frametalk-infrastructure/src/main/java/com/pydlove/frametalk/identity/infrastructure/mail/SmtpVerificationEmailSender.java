package com.pydlove.frametalk.identity.infrastructure.mail;

import com.pydlove.frametalk.identity.domain.model.Email;
import com.pydlove.frametalk.identity.domain.port.VerificationEmailSender;
import com.pydlove.frametalk.shared.domain.DomainException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

@Component
@ConditionalOnProperty(
        name = "frametalk.mail.mode",
        havingValue = "smtp")
public class SmtpVerificationEmailSender implements VerificationEmailSender {

    private static final Logger log =
            LoggerFactory.getLogger(SmtpVerificationEmailSender.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String fromName;

    public SmtpVerificationEmailSender(
            JavaMailSender mailSender,
            @Value("${frametalk.mail.from:${spring.mail.username:}}") String from,
            @Value("${frametalk.mail.from-name:帧言}") String fromName,
            @Value("${frametalk.mail.trust-store:}") String trustStorePath,
            @Value("${frametalk.mail.trust-store-password:}") String trustStorePassword) {
        this.mailSender = mailSender;
        this.from = from == null ? "" : from.trim();
        this.fromName = fromName == null ? "帧言" : fromName.trim();
        if (mailSender instanceof JavaMailSenderImpl javaMailSender) {
            var resolvedTrustStore = FrameTalkMailTls.resolveTrustStore(trustStorePath);
            if (resolvedTrustStore != null && !trustStorePassword.isBlank()) {
                Properties properties = javaMailSender.getJavaMailProperties();
                properties.put(
                        "mail.smtp.ssl.socketFactory",
                        FrameTalkMailTls.socketFactory(
                                resolvedTrustStore.toString(),
                                trustStorePassword));
                properties.put("mail.smtp.ssl.checkserveridentity", "true");
            }
        }
    }

    @Override
    public void sendRegistrationCode(Email email, String rawCode, Duration ttl) {
        if (from.isBlank()) {
            throw new DomainException("邮件发件人未配置，请设置 FRAMETALK_MAIL_FROM");
        }
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    false,
                    StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(from, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(email.value());
            helper.setSubject("帧言注册验证码");
            helper.setText(html(rawCode, ttl), true);
            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException error) {
            log.warn(
                    "FrameTalk verification email failed for {}: {}",
                    email.value(),
                    error.getMessage());
            throw new DomainException("验证码邮件发送失败，请检查邮件服务配置");
        } catch (MailException error) {
            log.warn(
                    "FrameTalk verification email failed for {}: {}",
                    email.value(),
                    error.getMessage());
            throw new DomainException("验证码邮件发送失败，请检查 SMTP 配置");
        }
    }

    private static String html(String rawCode, Duration ttl) {
        long minutes = Math.max(1, ttl.toMinutes());
        return """
                <!doctype html>
                <html lang="zh-CN">
                  <body style="margin:0;background:#f3efe4;color:#17251f;font-family:-apple-system,BlinkMacSystemFont,'PingFang SC',sans-serif;">
                    <div style="max-width:520px;margin:0 auto;padding:40px 24px;">
                      <div style="background:#ffffff;border-radius:14px;padding:32px;box-shadow:0 10px 36px rgba(23,37,31,.10);">
                        <p style="margin:0 0 8px;color:#ff5b35;font-size:12px;font-weight:700;letter-spacing:.08em;">FRAMETALK</p>
                        <h1 style="margin:0;font-size:24px;line-height:1.3;">帧言注册验证码</h1>
                        <p style="margin:14px 0 24px;color:#5e6a64;font-size:14px;line-height:1.7;">你正在创建帧言账号，请在注册页面输入以下验证码。</p>
                        <div style="padding:18px 20px;background:#17251f;border-radius:10px;color:#f3efe4;font-family:ui-monospace,Menlo,monospace;font-size:32px;font-weight:700;letter-spacing:.16em;text-align:center;">%s</div>
                        <p style="margin:20px 0 0;color:#7a847f;font-size:12px;line-height:1.6;">验证码 %d 分钟内有效，仅可使用一次。若不是你本人操作，请忽略这封邮件。</p>
                      </div>
                    </div>
                  </body>
                </html>
                """.formatted(rawCode, minutes);
    }
}
