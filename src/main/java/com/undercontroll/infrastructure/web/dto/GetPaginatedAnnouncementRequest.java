package com.undercontroll.infrastructure.web.dto;

public record GetPaginatedAnnouncementRequest(
        Integer page,
        Integer size
) {
}
