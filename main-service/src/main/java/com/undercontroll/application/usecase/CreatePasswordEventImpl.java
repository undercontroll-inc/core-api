package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.CreatePasswordEventPort;
import com.undercontroll.domain.entity.PasswordEvent;
import com.undercontroll.domain.entity.enums.PasswordEventStatus;
import com.undercontroll.domain.entity.enums.PasswordEventType;
import com.undercontroll.domain.exception.InvalidPasswordResetException;
import com.undercontroll.infrastructure.persistence.repository.PasswordEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreatePasswordEventImpl implements CreatePasswordEventPort {

    private final PasswordEventRepository repository;

    @Override
    public Output execute(Input input) {
        String value = input.userPhone();

        if (input.type().equals(PasswordEventType.RESET)) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastWeek = now.minusDays(7);

            boolean alreadyChangedThePasswordInTheLastWeek = !repository
                    .findByCreatedAtBetweenAndType(lastWeek, now, PasswordEventType.RESET).isEmpty();

            if (alreadyChangedThePasswordInTheLastWeek) {
                throw new InvalidPasswordResetException("Password has already been reset in the interval of a week");
            }

            PasswordEvent activePassword = repository.findByStatusAndType(PasswordEventStatus.ACTIVE, PasswordEventType.RESET);

            if (activePassword != null) {
                activePassword.setStatus(PasswordEventStatus.USED);
                repository.save(activePassword);
            }

            value = input.value();
        }

        PasswordEvent passwordEvent = PasswordEvent.builder()
                .id(UUID.randomUUID())
                .type(input.type())
                .status(PasswordEventStatus.ACTIVE)
                .userAgent(input.agent())
                .value(value)
                .userPhone(input.userPhone())
                .build();

        PasswordEvent saved = repository.save(passwordEvent);

        return new Output(
                saved.getId().toString(),
                saved.getType(),
                saved.getValue(),
                saved.getUserPhone()
        );
    }
}
