package com.undercontroll.domain.usecase.announcement;

import com.undercontroll.domain.enums.AnnouncementType;

import java.util.List;

public interface GetAnnouncementsPort {

    Output execute(Integer page, Integer size, AnnouncementType type);

    record Output(
            List<AnnouncementOutput> announcements,
            long totalElements,
            int totalPages,
            Integer page,
            Integer size
    ) {
    }
}
