package com.undercontroll.infrastructure.adapter;

import com.undercontroll.application.service.EmailService;
import com.undercontroll.domain.exception.MailSendingException;
import com.undercontroll.events.AnnouncementCreatedEvent;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEvent;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
            String body,
            ApplicationEvent event
    ) {
        log.info("Sending email to {}, subject {}, body {}", to, subject, body);

        try{
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);

            String html = "";

            switch (event) {
                case AnnouncementCreatedEvent e ->
                        html = this.buildAnnouncementCreatedHtml(e, "announcement_created");
                default ->
                        log.info("Default");
            }

            helper.setText(html, true);

            mailSender.send(message);

        } catch(Exception e) {

            throw new MailSendingException(
                    "Houve um erro ao enviar o email para %s: %s".formatted(to, e.getMessage())
            );
        }

    }

    private String buildAnnouncementCreatedHtml(
            AnnouncementCreatedEvent announcement,
            String htmlName
    ){
        String template = loadTemplate(htmlName + ".html");

        return template
                .replace("{{type}}", announcement.getAnnouncement().getType().toString())
                .replace("{{title}}", announcement.getAnnouncement().getTitle())
                .replace("{{content}}", announcement.getAnnouncement().getContent())
                .replace("{{createdAt}}",this.formatDateTime(announcement.getAnnouncement().getPublishedAt())
                        .replace("{{year}}", "2025")
                        .replace("{{websiteUrl}}", this.websiteUrl)
                        .replace("{{contactUrl}}", this.contactUrl));
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

        return dateTime.format(formatter);
    }


    private String loadTemplate(String templateName) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("templates/" + templateName)){

            if (inputStream == null) {
                throw new IllegalArgumentException("Template not found: " + templateName);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error loading the template: {}", templateName, e);
            throw new RuntimeException("Error while loading the template", e);
        }
    }

}
