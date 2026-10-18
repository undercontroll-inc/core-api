package com.undercontroll.domain.event;

import com.undercontroll.domain.model.Announcement;

public record AnnouncementCreatedEvent (
        Announcement announcement,
        String token
) {
}