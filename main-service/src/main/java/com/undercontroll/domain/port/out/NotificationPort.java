package com.undercontroll.domain.port.out;

import com.undercontroll.domain.events.AnnouncementCreatedEvent;

public interface NotificationPort {

    void handleAnnouncementCreated(AnnouncementCreatedEvent event);

}
