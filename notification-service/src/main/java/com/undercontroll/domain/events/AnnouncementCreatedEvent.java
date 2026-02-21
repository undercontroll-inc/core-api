package com.undercontroll.domain.events;

import java.time.LocalDateTime;

public record AnnouncementCreatedEvent(
        Integer id,
        String title,
        String content,
        String type,
        LocalDateTime publishedAt,
        String token
) {

}