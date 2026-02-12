package com.undercontroll.infrastructure.web.dto;

import java.util.List;

public record GetPaginatedAnnouncementResponse(
        List<AnnouncementDto> announcements,
        Integer page,
        Integer size
) {
}
