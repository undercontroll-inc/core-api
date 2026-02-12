package com.undercontroll.infrastructure.messaging.listener;

import com.undercontroll.infrastructure.web.dto.UserDto;
import com.undercontroll.infrastructure.messaging.event.AnnouncementCreatedEvent;
import com.undercontroll.application.service.EmailService;
import com.undercontroll.application.service.UserService;
import com.undercontroll.application.service.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class AnnouncementCreatedListener {

    private final EmailService emailService;
    private final UserService userService;
    private final MetricsService metricsService;

    @Async("taskExecutor")
    @EventListener
    public void handleNewAnnouncementEvent(AnnouncementCreatedEvent event) {

        List<UserDto> users = userService.findAllCustomersThatHaveEmail();

        // Notifica os usuários que possuem email neste momento
        users.forEach(user -> {
            emailService.sendEmail(
                    user.email(),
                    event.getAnnouncement().getTitle(),
                    event.getAnnouncement().getContent(),
                    event
            );
        });

        metricsService.incrementAnnouncementEmailsSent(users.size());

    }

}
