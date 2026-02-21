package com.undercontroll.domain.port.in;

import com.undercontroll.domain.events.AnnouncementCreatedEvent;

public interface AnnouncementCreatedPort {

    void execute(
            AnnouncementCreatedEvent event
    );

}
