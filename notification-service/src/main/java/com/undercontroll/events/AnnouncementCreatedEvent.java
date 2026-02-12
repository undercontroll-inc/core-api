package com.undercontroll.events;

import com.undercontroll.model.enums.AnnouncementType;

import java.time.LocalDateTime;

public record AnnouncementCreatedEvent(
    LocalDateTime createdAt,
    String content,
    String title,
    AnnouncementType type
) {

}