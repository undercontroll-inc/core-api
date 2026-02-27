package com.undercontroll.infrastructure.persistence.adapter;

import com.undercontroll.domain.model.PasswordEvent;
import com.undercontroll.domain.model.enums.PasswordEventStatus;
import com.undercontroll.domain.model.enums.PasswordEventType;
import com.undercontroll.domain.port.out.PasswordEventRepositoryPort;
import com.undercontroll.infrastructure.persistence.entity.PasswordEventJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.PasswordEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PasswordEventRepositoryAdapter implements PasswordEventRepositoryPort {

    private final PasswordEventRepository passwordEventRepository;

    @Override
    public PasswordEvent save(PasswordEvent passwordEvent) {
        PasswordEventJpaEntity jpaEntity = PasswordEventJpaEntity.fromDomain(passwordEvent);
        PasswordEventJpaEntity savedEntity = passwordEventRepository.save(jpaEntity);
        return savedEntity.toDomain();
    }

    @Override
    public void deleteById(UUID id) {
        passwordEventRepository.deleteById(id);
    }

    @Override
    public Optional<PasswordEvent> findById(UUID id) {
        return passwordEventRepository.findById(id).map(PasswordEventJpaEntity::toDomain);
    }

    @Override
    public List<PasswordEvent> findAll() {
        return passwordEventRepository.findAll().stream()
                .map(PasswordEventJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<PasswordEvent> findByUserPhone(String userPhone) {
        return passwordEventRepository.findByUserPhone(userPhone).stream()
                .map(PasswordEventJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<PasswordEvent> findByCreatedAtBetweenAndType(LocalDateTime start, LocalDateTime end, PasswordEventType type) {
        return passwordEventRepository.findByCreatedAtBetweenAndType(start, end, type).stream()
                .map(PasswordEventJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<PasswordEvent> findByStatusAndType(PasswordEventStatus status, PasswordEventType type) {
        return Optional.ofNullable(passwordEventRepository.findByStatusAndType(status, type))
                .map(PasswordEventJpaEntity::toDomain);
    }

}
