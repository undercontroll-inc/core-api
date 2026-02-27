package com.undercontroll.infrastructure.web.dto;

import com.undercontroll.application.dto.AnnouncementDto;

import java.util.List;

public record GetPaginatedAnnouncementResponse(
        List<AnnouncementDto> announcements,
        Integer page,
        Integer size
) {
}
