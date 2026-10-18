package com.undercontroll.domain.usecase.user.impl;

import com.undercontroll.domain.enums.UserType;
import com.undercontroll.domain.exception.InvalidUserException;
import com.undercontroll.domain.exception.UserNotFoundException;
import com.undercontroll.domain.gateway.UserGateway;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.usecase.user.UpdateUserPort;
import com.undercontroll.domain.usecase.user.UserOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserUseCasesTest {

    @Mock
    private UserGateway userGateway;

    private static User user(int id, String name) {
        return User.builder().id(id).name(name).email(name + "@x.com").password("hash").userType(UserType.CUSTOMER).build();
    }

    @Test
    @DisplayName("get user maps to output without the password and validates the id")
    void getUser() {
        when(userGateway.findById(1)).thenReturn(Optional.of(user(1, "ana")));
        when(userGateway.findById(2)).thenReturn(Optional.empty());
        GetUserImpl useCase = new GetUserImpl(userGateway);

        UserOutput output = useCase.execute(1).orElseThrow();

        assertEquals("ana@x.com", output.email());
        assertTrue(useCase.execute(2).isEmpty());
        assertThrows(InvalidUserException.class, () -> useCase.execute(null));
    }

    @Test
    @DisplayName("list users picks the query by type and email filter")
    void getUsers() {
        when(userGateway.findAllCustomersThatHaveEmail()).thenReturn(List.of(user(1, "ana")));
        when(userGateway.findAllCustomers()).thenReturn(List.of(user(1, "ana"), user(2, "bia")));
        when(userGateway.findAll()).thenReturn(List.of(user(1, "ana"), user(2, "bia"), user(3, "caio")));
        GetUsersImpl useCase = new GetUsersImpl(userGateway);

        assertEquals(1, useCase.execute(UserType.CUSTOMER, true).size());
        assertEquals(2, useCase.execute(UserType.CUSTOMER, null).size());
        assertEquals(3, useCase.execute(null, null).size());
    }

    @Test
    @DisplayName("update overwrites only provided fields")
    void update() {
        User existing = user(1, "ana");
        when(userGateway.findById(1)).thenReturn(Optional.of(existing));

        new UpdateUserImpl(userGateway).execute(1, new UpdateUserPort.Input(
                "Ana", "Souza", "new", "Rua 2", "123", "1198", "avatar", "01000-000", true, false, false, UserType.ADMINISTRATOR
        ));

        assertEquals("Ana", existing.getName());
        assertEquals("Souza", existing.getLastName());
        assertEquals("Rua 2", existing.getAddress());
        assertEquals("01000-000", existing.getCEP());
        assertEquals("1198", existing.getPhone());
        assertEquals("avatar", existing.getAvatarUrl());
        assertEquals(UserType.ADMINISTRATOR, existing.getUserType());
        assertEquals(true, existing.getAlreadyRecurrent());
        verify(userGateway).save(existing);
    }

    @Test
    @DisplayName("update and delete fail for unknown users")
    void unknownUser() {
        when(userGateway.findById(9)).thenReturn(Optional.empty());
        UpdateUserPort.Input input = new UpdateUserPort.Input(null, null, null, null, null, null, null, null, null, null, null, null);

        assertThrows(UserNotFoundException.class, () -> new UpdateUserImpl(userGateway).execute(9, input));
        assertThrows(UserNotFoundException.class, () -> new DeleteUserImpl(userGateway).execute(9));
        assertThrows(InvalidUserException.class, () -> new DeleteUserImpl(userGateway).execute(null));
    }
}
