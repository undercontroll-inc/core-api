package com.undercontroll.infrastructure.seed;

import com.undercontroll.domain.gateway.AnnouncementGateway;
import com.undercontroll.domain.gateway.ComponentGateway;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.gateway.UserGateway;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserGateway userGateway;
    @Mock
    private ComponentGateway componentGateway;
    @Mock
    private AnnouncementGateway announcementGateway;
    @Mock
    private OrderGateway orderGateway;
    @Mock
    private PasswordEncoder encoder;

    private DataInitializer initializer(String password) {
        return new DataInitializer(userGateway, componentGateway, announcementGateway, orderGateway, encoder, password);
    }

    @Test
    @DisplayName("only runs in the dev profile")
    void devProfileOnly() {
        assertThat(DataInitializer.class.getAnnotation(Profile.class).value()).containsExactly("dev");
    }

    @Test
    @DisplayName("skips seeding without DEV_SEED_PASSWORD")
    void skipsWithoutPassword() {
        initializer(" ").init();
        initializer(null).init();

        verifyNoInteractions(userGateway, componentGateway, announcementGateway, orderGateway, encoder);
    }

    @Test
    @DisplayName("skips seeding when the admin already exists, without scanning the user table")
    void skipsWhenSeeded() {
        when(userGateway.findByEmail(DataInitializer.ADMIN_EMAIL)).thenReturn(Optional.of(new User()));

        initializer("secret").init();

        verify(userGateway, never()).save(any());
        verifyNoInteractions(componentGateway, orderGateway);
    }

    @Test
    @DisplayName("seeds users with the configured password, components, an announcement and orders")
    void seeds() {
        when(userGateway.findByEmail(DataInitializer.ADMIN_EMAIL)).thenReturn(Optional.empty());
        when(encoder.encode("secret")).thenReturn("hashed");
        when(userGateway.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(componentGateway.save(any(ComponentPart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        initializer("secret").init();

        ArgumentCaptor<User> users = ArgumentCaptor.forClass(User.class);
        verify(userGateway, times(6)).save(users.capture());
        assertThat(users.getAllValues()).allMatch(user -> "hashed".equals(user.getPassword()));
        verify(componentGateway, times(7)).save(any(ComponentPart.class));
        verify(announcementGateway).save(any());
        ArgumentCaptor<Order> orders = ArgumentCaptor.forClass(Order.class);
        verify(orderGateway, atLeastOnce()).save(orders.capture());
        assertThat(orders.getAllValues()).hasSize(20).allMatch(order -> order.getOrderItems().size() == 1);
    }
}
