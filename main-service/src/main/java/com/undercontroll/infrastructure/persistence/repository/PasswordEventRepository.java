package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.domain.model.enums.PasswordEventStatus;
import com.undercontroll.domain.model.enums.PasswordEventType;
import com.undercontroll.infrastructure.persistence.entity.PasswordEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PasswordEventRepository extends JpaRepository<PasswordEventJpaEntity, UUID> {

    List<PasswordEventJpaEntity> findByUserPhone(String userPhone);
    List<PasswordEventJpaEntity> findByCreatedAtBetweenAndType(LocalDateTime start, LocalDateTime end, PasswordEventType type);

    PasswordEventJpaEntity findByStatusAndType(PasswordEventStatus status, PasswordEventType type);

}
