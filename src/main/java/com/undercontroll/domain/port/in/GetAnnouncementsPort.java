package com.undercontroll.domain.port.in;

import com.undercontroll.application.dto.AnnouncementDto;

import java.util.List;

public interface GetAnnouncementsPort {
    record Input(
            Integer page,
            Integer size
    ) {}

    record Output(
            List<AnnouncementDto> announcements
    ) {}

    Output execute(Input input);
}
