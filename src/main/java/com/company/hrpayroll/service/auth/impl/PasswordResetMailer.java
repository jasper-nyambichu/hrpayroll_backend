package com.company.hrpayroll.service.auth.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Sends password reset emails. Asynchronous, so a request for a real account takes as long to answer as one
 * for an unknown account, and a mail server fault cannot reveal which is which. Failures only reach the log.
 *
 * The code travels after the # in the button's link. Browsers never send that part to a server, so it stays
 * out of web logs, and a mail scanner that fetches the link learns nothing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetMailer {

    private final JavaMailSender mailSender;
    private final com.company.hrpayroll.repository.company.CompanyRepository companyRepository;

    // The organisation's name from Admin → Settings, so the emails carry the client's name, not the vendor's.
    private String systemName() {
        return companyRepository.findAll().stream().findFirst()
                .map(company -> company.getName())
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .orElse("staff portal");
    }

    @Value("${app.mail.from:no-reply@dhwarsh.local}")
    private String from;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Async
    public void sendCode(String to, String name, String username, String code, int minutesValid) {
        String link = frontendUrl.replaceAll("/$", "") + "/forgot-password/verify#u="
                + URLEncoder.encode(username, StandardCharsets.UTF_8) + "&c=" + code;
        String safeName = HtmlUtils.htmlEscape(name);
        String safeLink = HtmlUtils.htmlEscape(link);

        String html = """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#1b1b1f">
                  <h2 style="margin:0 0 12px;color:#0b2a45">Reset your password</h2>
                  <p style="margin:0 0 16px">Hello %s, we received a request to reset your %s password.</p>
                  <p style="margin:0 0 20px">
                    <a href="%s" style="display:inline-block;background:#0b2a45;color:#ffffff;text-decoration:none;font-weight:bold;padding:12px 24px;border-radius:8px">Continue to reset</a>
                  </p>
                  <p style="margin:0 0 8px">Or enter this code on the reset page:</p>
                  <p style="margin:0 0 20px;font-size:32px;font-weight:bold;letter-spacing:8px">%s</p>
                  <p style="margin:0 0 8px;color:#55575c;font-size:13px">It works for %d minutes.</p>
                  <p style="margin:0;color:#55575c;font-size:13px">If you did not ask for this, ignore this email. Your password stays the same. Never share this code with anyone.</p>
                </div>
                """.formatted(safeName, HtmlUtils.htmlEscape(systemName()), safeLink, code, minutesValid);
        String text = "Hello " + name + ",\n\nUse this code to reset your " + systemName() + " password: " + code
                + "\nOr open this link: " + link
                + "\n\nIt works for " + minutesValid + " minutes. If you did not ask for this, ignore this email.";

        send(to, "Your " + systemName() + " password reset code: " + code, text, html);
    }

    @Async
    public void sendPasswordChanged(String to, String name) {
        String text = "Hello " + name + ",\n\nYour " + systemName() + " password has just been changed.\n"
                + "If this was not you, contact your administrator straight away.";
        String html = "<div style=\"font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#1b1b1f\">"
                + "<h2 style=\"margin:0 0 12px;color:#0b2a45\">Your password was changed</h2>"
                + "<p>Hello " + HtmlUtils.htmlEscape(name) + ", your " + HtmlUtils.htmlEscape(systemName()) + " password has just been changed.</p>"
                + "<p>If this was not you, contact your administrator straight away.</p></div>";
        send(to, "Your " + systemName() + " password was changed", text, html);
    }

    private void send(String to, String subject, String text, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html);
            mailSender.send(message);
        } catch (MessagingException | MailException ex) {
            log.error("Could not send the email '{}': {}", subject.replaceAll("\\d{6}", "******"), ex.getMessage());
        }
    }
}
