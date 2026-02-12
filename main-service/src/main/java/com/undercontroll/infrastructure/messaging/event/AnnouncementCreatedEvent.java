package com.undercontroll.infrastructure.messaging.event;

import com.undercontroll.domain.entity.Announcement;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AnnouncementCreatedEvent extends ApplicationEvent {

    private final Announcement announcement;

    public AnnouncementCreatedEvent(
            Object source,
            Announcement announcement
    ) {
        super(source);
        this.announcement = announcement;
    }
}
