package com.undercontroll.application.usecase;

import com.undercontroll.application.dto.auth.CreatePasswordEventRequest;
import com.undercontroll.application.dto.user.CreateUserRequest;
import com.undercontroll.application.dto.user.CreateUserResponse;
import com.undercontroll.application.mapper.UserDtoMapper;
import com.undercontroll.domain.gateway.MetricsGateway;
import com.undercontroll.domain.gateway.NotificationGateway;
import com.undercontroll.domain.usecase.auth.CreatePasswordEventPort;
import com.undercontroll.domain.usecase.user.impl.CreateUserImpl;
import com.undercontroll.domain.enums.PasswordEventType;
import com.undercontroll.domain.enums.UserType;
import com.undercontroll.infrastructure.events.UserCreatedEvent;
import com.undercontroll.domain.model.PasswordEvent;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.gateway.UserGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateUserImplTest {

    @Mock
    private UserGateway userGateway;

    @Mock
    private CreatePasswordEventPort createPasswordEventPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MetricsGateway metricsGateway;

    @Mock
    private NotificationGateway notificationGateway;

    @Mock
    private UserDtoMapper userDtoMapper;

    @InjectMocks
    private CreateUserImpl useCase;

    @Test
    void execute_shouldPublishUserCreatedEvent_whenUserTypeIsCustomer() {
        CreateUserRequest request = new CreateUserRequest(
                "Maria",
                "maria@teste.com",
                "11999999999",
                "Silva",
                "any-pass",
                "Rua A",
                "12345678901",
                null,
                UserType.CUSTOMER,
                true,
                false,
                true,
                "03143000"
        );

        when(userGateway.findByEmail(request.email())).thenReturn(Optional.empty());
        when(userGateway.findByPhone(request.phone())).thenReturn(Optional.empty());
        when(userGateway.findByCpf(request.cpf())).thenReturn(Optional.empty());
        when(createPasswordEventPort.execute(any(CreatePasswordEventRequest.class)))
                .thenReturn(PasswordEvent.builder()
                        .type(PasswordEventType.CREATE)
                        .value("generated-pass")
                        .userPhone(request.phone())
                        .build());
        when(passwordEncoder.encode("generated-pass")).thenReturn("encoded-generated-pass");

        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 30, 10, 30);
        User savedUser = User.builder()
                .name(request.name())
                .email(request.email())
                .userType(UserType.CUSTOMER)
                .createdAt(createdAt)
                .build();
        when(userGateway.save(any(User.class))).thenReturn(savedUser);
        when(userDtoMapper.toCreateUserResponse(savedUser)).thenReturn(new CreateUserResponse(
                savedUser.getName(), savedUser.getEmail(), null, null, null, null, null, null, UserType.CUSTOMER
        ));

        useCase.execute(request);

        ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(notificationGateway).handleUserCreated(eventCaptor.capture());
        UserCreatedEvent event = eventCaptor.getValue();

        assertThat(event.name()).isEqualTo("Maria");
        assertThat(event.email()).isEqualTo("maria@teste.com");
        assertThat(event.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void execute_shouldNotPublishUserCreatedEvent_whenUserTypeIsAdministrator() {
        CreateUserRequest request = new CreateUserRequest(
                "Admin",
                "admin@teste.com",
                "11888888888",
                "Master",
                "admin-pass",
                "Rua B",
                "10987654321",
                null,
                UserType.ADMINISTRATOR,
                false,
                false,
                false,
                "03143000"
        );

        when(userGateway.findByEmail(request.email())).thenReturn(Optional.empty());
        when(userGateway.findByPhone(request.phone())).thenReturn(Optional.empty());
        when(userGateway.findByCpf(request.cpf())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-admin-pass");
        User savedUser = User.builder()
                .name(request.name())
                .email(request.email())
                .userType(UserType.ADMINISTRATOR)
                .createdAt(LocalDateTime.now())
                .build();
        when(userGateway.save(any(User.class))).thenReturn(savedUser);
        when(userDtoMapper.toCreateUserResponse(savedUser)).thenReturn(new CreateUserResponse(
                savedUser.getName(), savedUser.getEmail(), null, null, null, null, null, null, UserType.ADMINISTRATOR
        ));

        useCase.execute(request);

        verify(notificationGateway, never()).handleUserCreated(any(UserCreatedEvent.class));
    }
}
