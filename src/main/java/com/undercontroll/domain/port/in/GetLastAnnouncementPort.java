package com.undercontroll.domain.port.in;

import com.undercontroll.application.dto.AnnouncementDto;

import java.util.Optional;

public interface GetLastAnnouncementPort {
    Optional<AnnouncementDto> execute();
}
