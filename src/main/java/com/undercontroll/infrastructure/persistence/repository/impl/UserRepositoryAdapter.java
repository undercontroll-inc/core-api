package com.undercontroll.infrastructure.persistence.repository.impl;

import com.undercontroll.domain.model.User;
import com.undercontroll.domain.repository.UserRepositoryPort;
import com.undercontroll.infrastructure.persistence.entity.UserJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(User user) {
        UserJpaEntity jpaEntity = UserJpaEntity.fromDomain(user);
        UserJpaEntity savedEntity = userJpaRepository.save(jpaEntity);
        return savedEntity.toDomain();
    }

    @Override
    public void deleteById(Integer id) {
        userJpaRepository.deleteById(id);
    }

    @Override
    public Optional<User> findById(Integer id) {
        return userJpaRepository.findById(id).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findUserByEmail(email).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        return userJpaRepository.findUserByPhone(phone).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByCpf(String cpf) {
        return userJpaRepository.findUserByCpf(cpf).map(UserJpaEntity::toDomain);
    }

    @Override
    public List<User> findAll() {
        return userJpaRepository.findAll().stream()
                .map(UserJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<User> findAllCustomers() {
        return userJpaRepository.findAllCustomers().stream()
                .map(UserJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<User> findCustomerById(Integer id) {
        return userJpaRepository.findCustomerById(id).map(UserJpaEntity::toDomain);
    }

    @Override
    public List<User> findAllCustomersThatHaveEmail() {
        return userJpaRepository.findAllCustomersThatHaveEmail().stream()
                .map(UserJpaEntity::toDomain)
                .toList();
    }

}
