package com.undercontroll.domain.gateway;

import com.undercontroll.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserGateway {

    User save(User user);

    void deleteById(Integer id);

    Optional<User> findById(Integer id);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByCpf(String cpf);

    List<User> findAll(int limit);

    List<User> findCustomers(int limit);

    Optional<User> findCustomerById(Integer id);

    List<User> findCustomersWithEmail(int limit);

}
