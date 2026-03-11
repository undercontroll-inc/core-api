package com.undercontroll.presentation.dto;

import com.undercontroll.application.dto.AnnouncementDto;

import java.util.List;

public record GetPaginatedAnnouncementResponse(
        List<AnnouncementDto> announcements,
        Integer page,
        Integer size
) {
}
