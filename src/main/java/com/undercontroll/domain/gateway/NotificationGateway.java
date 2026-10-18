package com.undercontroll.domain.gateway;

import com.undercontroll.domain.event.AnnouncementCreatedEvent;
import com.undercontroll.domain.event.UserCreatedEvent;

public interface NotificationGateway {

    void handleAnnouncementCreated(AnnouncementCreatedEvent event);

    void handleUserCreated(UserCreatedEvent event);

}
