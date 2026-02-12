package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.domain.entity.PasswordEvent;
import com.undercontroll.domain.entity.enums.PasswordEventStatus;
import com.undercontroll.domain.entity.enums.PasswordEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PasswordEventRepository extends JpaRepository<PasswordEvent, UUID> {

    List<PasswordEvent> findByUserPhone(String userPhone);
    List<PasswordEvent> findByCreatedAtBetweenAndType(LocalDateTime start, LocalDateTime end, PasswordEventType type);

    PasswordEvent findByStatusAndType(PasswordEventStatus status, PasswordEventType type);

}
