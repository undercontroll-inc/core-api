package com.undercontroll.presentation.dto;

public record GetPaginatedAnnouncementRequest(
        Integer page,
        Integer size
) {
}
