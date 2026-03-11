package com.undercontroll.domain.repository;

import com.undercontroll.domain.model.PasswordEvent;
import com.undercontroll.domain.enums.PasswordEventStatus;
import com.undercontroll.domain.enums.PasswordEventType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordEventRepositoryPort {

    PasswordEvent save(PasswordEvent passwordEvent);

    void deleteById(UUID id);

    Optional<PasswordEvent> findById(UUID id);

    List<PasswordEvent> findAll();

    List<PasswordEvent> findByUserPhone(String userPhone);

    List<PasswordEvent> findByCreatedAtBetweenAndType(LocalDateTime start, LocalDateTime end, PasswordEventType type);

    Optional<PasswordEvent> findByStatusAndType(PasswordEventStatus status, PasswordEventType type);

}
