package com.undercontroll.application.usecase;

import com.undercontroll.application.service.EmailService;
import com.undercontroll.domain.port.in.AnnouncementCreatedPort;
import com.undercontroll.domain.port.out.EmailTemplateLoader;
import com.undercontroll.domain.events.AnnouncementCreatedEvent;
import com.undercontroll.infrastructure.client.MainServiceClient;
import com.undercontroll.infrastructure.client.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class AnnouncementCreatedImpl implements AnnouncementCreatedPort {

    private final EmailService emailService;
    private final EmailTemplateLoader emailTemplateLoader;
    private final MainServiceClient mainServiceClient;

    private static final String HTML_NAME = "announcement_created.html";

    @Override
    public void execute(AnnouncementCreatedEvent event) {
        log.info("Sending emails for new announcement: {}", event.title());

        String template = buildTemplate(event);

        List<UserDto> users = mainServiceClient.getCustomersThatHaveEmail(event.token());

        if (users != null) {
            for (UserDto user : users) {
                try {
                    emailService.sendEmail(
                            user.email(),
                            "Novo Aviso: " + event.title(),
                            template
                    );
                    log.info("Email sent to {}", user.email());
                } catch (Exception e) {
                    log.error("Failed to send email to {}", user.email(), e);
                }
            }
        }
    }

    private String buildTemplate(AnnouncementCreatedEvent event) {
        String template = emailTemplateLoader.load(HTML_NAME);

        return template
                .replace("{{type}}", event.type() != null ? event.type() : "")
                .replace("{{title}}", event.title() != null ? event.title() : "")
                .replace("{{content}}", event.content() != null ? event.content() : "")
                .replace("{{createdAt}}", this.formatDateTime(event.publishedAt()))
                .replace("{{year}}", "2025");
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

        return dateTime.format(formatter);
    }
}
