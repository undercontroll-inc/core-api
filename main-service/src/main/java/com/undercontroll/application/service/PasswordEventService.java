package com.undercontroll.application.service;

import com.undercontroll.infrastructure.web.dto.CreatePasswordEventRequest;
import com.undercontroll.domain.exception.InvalidPasswordResetException;
import com.undercontroll.domain.entity.PasswordEvent;
import com.undercontroll.domain.entity.enums.PasswordEventStatus;
import com.undercontroll.domain.entity.enums.PasswordEventType;
import com.undercontroll.infrastructure.persistence.repository.PasswordEventRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Service
public class PasswordEventService {

    private final PasswordEventRepository repository;

    public PasswordEvent create(CreatePasswordEventRequest request) {

        String value = request.userPhone();

        if(request.type().equals(PasswordEventType.RESET)){
            LocalDateTime now = LocalDateTime.now();

            LocalDateTime lastWeek = now.minusDays(7);

            boolean alreadyChangedThePasswordInTheLastWeek = !this.repository
                    .findByCreatedAtBetweenAndType(lastWeek, now, PasswordEventType.RESET).isEmpty();

            if(alreadyChangedThePasswordInTheLastWeek) {
                throw new InvalidPasswordResetException("Password has already been reset in the interval of a week");
            }

            PasswordEvent activePassword = repository.findByStatusAndType(PasswordEventStatus.ACTIVE, PasswordEventType.RESET);

            if(activePassword != null){
                activePassword.setStatus(PasswordEventStatus.USED);

                repository.save(activePassword);
            }

            value = request.value();
        }

        PasswordEvent passwordEvent = PasswordEvent.builder()
                .id(UUID.randomUUID())
                .type(request.type())
                .status(PasswordEventStatus.ACTIVE)
                .userAgent(request.agent())
                .value(value)
                .userPhone(request.userPhone())
                .build();

        return repository.save(passwordEvent);
    }

}
