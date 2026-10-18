package com.undercontroll.domain.usecase.announcement;

import java.util.Optional;

public interface GetLastAnnouncementPort {
    Optional<AnnouncementOutput> execute();
}
