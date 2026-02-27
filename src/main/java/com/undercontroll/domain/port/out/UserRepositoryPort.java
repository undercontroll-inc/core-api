package com.undercontroll.domain.port.out;

import com.undercontroll.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {

    User save(User user);

    void deleteById(Integer id);

    Optional<User> findById(Integer id);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByCpf(String cpf);

    List<User> findAll();

    List<User> findAllCustomers();

    Optional<User> findCustomerById(Integer id);

    List<User> findAllCustomersThatHaveEmail();

}
