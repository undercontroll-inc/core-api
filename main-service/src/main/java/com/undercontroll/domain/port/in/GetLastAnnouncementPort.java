package com.undercontroll.domain.port.in;

import com.undercontroll.infrastructure.web.dto.AnnouncementDto;

import java.util.Optional;

public interface GetLastAnnouncementPort {
    Optional<AnnouncementDto> execute();
}
