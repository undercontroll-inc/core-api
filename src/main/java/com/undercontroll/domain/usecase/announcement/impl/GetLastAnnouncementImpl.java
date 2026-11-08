package com.undercontroll.domain.usecase.announcement.impl;

import com.undercontroll.domain.gateway.AnnouncementGateway;
import com.undercontroll.domain.gateway.ObjectStorageGateway;
import com.undercontroll.domain.usecase.announcement.AnnouncementOutput;
import com.undercontroll.domain.usecase.announcement.AnnouncementOutputs;
import com.undercontroll.domain.usecase.announcement.GetLastAnnouncementPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetLastAnnouncementImpl implements GetLastAnnouncementPort {

    private final AnnouncementGateway announcementGateway;
    private final ObjectStorageGateway storageGateway;

    @Override
    public Optional<AnnouncementOutput> execute() {
        return announcementGateway.findLastAnnouncement()
                .map(announcement -> AnnouncementOutputs.from(announcement, storageGateway));
    }
}
