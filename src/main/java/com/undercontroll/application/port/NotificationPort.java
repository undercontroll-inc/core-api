package com.undercontroll.application.port;

import com.undercontroll.domain.events.AnnouncementCreatedEvent;

public interface NotificationPort {

    void handleAnnouncementCreated(AnnouncementCreatedEvent event);

}
