package com.undercontroll.domain.usecase.auth.impl;

import com.undercontroll.domain.enums.PasswordEventStatus;
import com.undercontroll.domain.enums.PasswordEventType;
import com.undercontroll.domain.exception.InvalidPasswordResetException;
import com.undercontroll.domain.gateway.PasswordEventGateway;
import com.undercontroll.domain.gateway.UserGateway;
import com.undercontroll.domain.model.PasswordEvent;
import com.undercontroll.domain.usecase.auth.CreatePasswordEventPort;
import com.undercontroll.domain.usecase.auth.ResetPasswordPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordUseCasesTest {

    @Mock
    private PasswordEventGateway passwordEventGateway;

    @Mock
    private UserGateway userGateway;

    @Test
    @DisplayName("create event stores the phone as the generated value")
    void createEvent() {
        when(passwordEventGateway.save(any(PasswordEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PasswordEvent event = new CreatePasswordEventImpl(passwordEventGateway)
                .execute(new CreatePasswordEventPort.Input(PasswordEventType.CREATE, "agent", "1199", "ignored"));

        assertEquals("1199", event.getValue());
        assertEquals(PasswordEventStatus.ACTIVE, event.getStatus());
        assertEquals("agent", event.getUserAgent());
    }

    @Test
    @DisplayName("reset event retires the active reset and stores the new value")
    void resetEvent() {
        PasswordEvent active = PasswordEvent.builder().status(PasswordEventStatus.ACTIVE).build();
        when(passwordEventGateway.findByCreatedAtBetweenAndType(any(), any(), eq(PasswordEventType.RESET))).thenReturn(List.of());
        when(passwordEventGateway.findByStatusAndType(PasswordEventStatus.ACTIVE, PasswordEventType.RESET)).thenReturn(Optional.of(active));
        when(passwordEventGateway.save(any(PasswordEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PasswordEvent event = new CreatePasswordEventImpl(passwordEventGateway)
                .execute(new CreatePasswordEventPort.Input(PasswordEventType.RESET, null, "1199", "new-pass"));

        assertEquals("new-pass", event.getValue());
        assertEquals(PasswordEventStatus.USED, active.getStatus());
        ArgumentCaptor<PasswordEvent> saved = ArgumentCaptor.forClass(PasswordEvent.class);
        verify(passwordEventGateway, org.mockito.Mockito.times(2)).save(saved.capture());
        assertEquals(active, saved.getAllValues().get(0));
    }

    @Test
    @DisplayName("reset is refused when one happened in the last week")
    void resetTooSoon() {
        when(passwordEventGateway.findByCreatedAtBetweenAndType(any(), any(), eq(PasswordEventType.RESET)))
                .thenReturn(List.of(new PasswordEvent()));
        CreatePasswordEventImpl useCase = new CreatePasswordEventImpl(passwordEventGateway);
        CreatePasswordEventPort.Input input = new CreatePasswordEventPort.Input(PasswordEventType.RESET, null, "1199", "x");

        assertThrows(InvalidPasswordResetException.class, () -> useCase.execute(input));
    }

    @Test
    @DisplayName("reset password validates user id, password and token")
    void resetPasswordValidation() {
        ResetPasswordImpl useCase = new ResetPasswordImpl(userGateway);
        ResetPasswordPort.Input valid = new ResetPasswordPort.Input("new", false);

        assertDoesNotThrow(() -> useCase.execute(1, valid, "token"));
        assertThrows(InvalidPasswordResetException.class, () -> useCase.execute(0, valid, "token"));
        assertThrows(InvalidPasswordResetException.class, () -> useCase.execute(1, new ResetPasswordPort.Input("", false), "token"));
        assertThrows(InvalidPasswordResetException.class, () -> useCase.execute(1, valid, ""));
    }
}
