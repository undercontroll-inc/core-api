package com.undercontroll.infrastructure.email;

import com.undercontroll.application.service.EmailService;
import com.undercontroll.domain.exception.MailSendingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Slf4j
@Service
public class JavaMailService implements EmailService {


    private final JavaMailSender mailSender;

    private final String contact = "contato@gmail.com";
    private final String year = String.valueOf(LocalDateTime.now().getYear());
    private final String websiteUrl = "IrmãosPelluci.com";
    private final String contactUrl = "contato@contato";

    @Value("${spring.mail.username}")
    private String from;

    @Override
    public void sendEmail(
            String to,
            String subject,
            String body
    ) {
        log.info("Sending email to {}, subject {}, body {}", to, subject, body);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);

            helper.setText(body, true);

            mailSender.send(message);

        } catch (Exception e) {
            throw new MailSendingException(
                    "Houve um erro ao enviar o email para %s: %s".formatted(to, e.getMessage())
            );
        }

    }
}
