package com.undercontroll.domain.gateway;

import com.undercontroll.infrastructure.events.AnnouncementCreatedEvent;
import com.undercontroll.infrastructure.events.UserCreatedEvent;

public interface NotificationGateway {

    void handleAnnouncementCreated(AnnouncementCreatedEvent event);

    void handleUserCreated(UserCreatedEvent event);

}
