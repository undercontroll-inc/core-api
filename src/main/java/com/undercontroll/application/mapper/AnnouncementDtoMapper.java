package com.undercontroll.application.mapper;

import com.undercontroll.application.dto.announcement.AnnouncementDto;
import com.undercontroll.application.dto.announcement.CreateAnnouncementRequest;
import com.undercontroll.application.dto.announcement.CreateAnnouncementResponse;
import com.undercontroll.application.dto.announcement.GenerateUploadUrlResponse;
import com.undercontroll.application.dto.announcement.GetPaginatedAnnouncementResponse;
import com.undercontroll.application.dto.announcement.UpdateAnnouncementRequest;
import com.undercontroll.application.dto.announcement.UpdateAnnouncementResponse;
import com.undercontroll.domain.model.PresignedUpload;
import com.undercontroll.domain.usecase.announcement.AnnouncementOutput;
import com.undercontroll.domain.usecase.announcement.CreateAnnouncementPort;
import com.undercontroll.domain.usecase.announcement.GetAnnouncementsPort;
import com.undercontroll.domain.usecase.announcement.UpdateAnnouncementPort;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AnnouncementDtoMapper {

    CreateAnnouncementPort.Input toInput(CreateAnnouncementRequest request);

    UpdateAnnouncementPort.Input toInput(UpdateAnnouncementRequest request);

    AnnouncementDto toDto(AnnouncementOutput output);

    GenerateUploadUrlResponse toDto(PresignedUpload upload);

    CreateAnnouncementResponse toResponse(CreateAnnouncementPort.Output output);

    UpdateAnnouncementResponse toResponse(UpdateAnnouncementPort.Output output);

    GetPaginatedAnnouncementResponse toResponse(GetAnnouncementsPort.Output output);
}
