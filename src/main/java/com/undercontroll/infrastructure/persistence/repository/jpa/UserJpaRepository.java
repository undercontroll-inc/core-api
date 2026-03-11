package com.undercontroll.infrastructure.persistence.repository.jpa;

import com.undercontroll.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Integer> {

    Optional<UserJpaEntity> findUserByEmail(String email);
    Optional<UserJpaEntity> findUserByPhone(String phone);
    Optional<UserJpaEntity> findUserByCpf(String cpf);

    @Query("SELECT u FROM UserJpaEntity u WHERE u.userType = 'CUSTOMER'")
    List<UserJpaEntity> findAllCustomers();

    @Query("SELECT u FROM UserJpaEntity u WHERE u.userType = 'CUSTOMER' AND u.id = :id")
    Optional<UserJpaEntity> findCustomerById(@Param("id") Integer id);

    @Query("SELECT u FROM UserJpaEntity u WHERE u.userType = 'CUSTOMER' AND u.email IS NOT NULL")
    List<UserJpaEntity> findAllCustomersThatHaveEmail();

}
