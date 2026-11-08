package com.undercontroll.application.mapper;

import com.undercontroll.application.dto.storage.UploadTicketDto;
import com.undercontroll.domain.storage.UploadTicket;
import org.springframework.stereotype.Component;

@Component
public class UploadDtoMapper {

    public UploadTicketDto toDto(UploadTicket ticket) {
        return new UploadTicketDto(ticket.uploadId(), ticket.upload().url(), ticket.upload().method(),
                ticket.upload().headers(), ticket.upload().expiresAt());
    }
}
