package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.User;
import com.undercontroll.infrastructure.mapper.ComponentPartMapper;
import com.undercontroll.infrastructure.mapper.UserMapper;
import com.undercontroll.infrastructure.persistence.entity.ComponentPartJpaEntity;
import com.undercontroll.infrastructure.persistence.entity.UserJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.ComponentJpaRepository;
import com.undercontroll.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListingGatewaysTest {

    @Test
    @DisplayName("user listings read a bounded first page ordered by id")
    void userListingsAreBounded() {
        UserJpaRepository repository = mock(UserJpaRepository.class);
        UserMapper mapper = mock(UserMapper.class);
        UserJpaEntity entity = new UserJpaEntity();
        User user = User.builder().id(1).build();
        when(mapper.toDomain(entity)).thenReturn(user);
        when(repository.findSlice(any(Pageable.class))).thenReturn(List.of(entity));
        when(repository.findCustomers(any(Pageable.class))).thenReturn(List.of(entity));
        when(repository.findCustomersWithEmail(any(Pageable.class))).thenReturn(List.of(entity));
        UserGatewayImpl gateway = new UserGatewayImpl(repository, mapper);

        assertThat(gateway.findAll(50)).containsExactly(user);
        assertThat(gateway.findCustomers(50)).containsExactly(user);
        assertThat(gateway.findCustomersWithEmail(0)).containsExactly(user);

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findSlice(page.capture());
        assertThat(page.getValue().getPageSize()).isEqualTo(50);
        assertThat(page.getValue().getSort()).isEqualTo(Sort.by("id"));
        verify(repository).findCustomersWithEmail(page.capture());
        assertThat(page.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("component listings read a bounded first page ordered by id")
    void componentListingsAreBounded() {
        ComponentJpaRepository repository = mock(ComponentJpaRepository.class);
        ComponentPartMapper mapper = mock(ComponentPartMapper.class);
        ComponentPartJpaEntity entity = new ComponentPartJpaEntity();
        ComponentPart part = ComponentPart.builder().id(1).build();
        when(mapper.toDomain(entity)).thenReturn(part);
        when(repository.findSlice(any(Pageable.class))).thenReturn(List.of(entity));
        when(repository.findByName(eq("Capacitor"), any(Pageable.class))).thenReturn(List.of(entity));
        when(repository.findByCategory(eq("Elétrica"), any(Pageable.class))).thenReturn(List.of(entity));
        ComponentGatewayImpl gateway = new ComponentGatewayImpl(repository, mapper);

        assertThat(gateway.findAll(500)).containsExactly(part);
        assertThat(gateway.findByName("Capacitor", 500)).containsExactly(part);
        assertThat(gateway.findByCategory("Elétrica", 500)).containsExactly(part);

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findSlice(page.capture());
        assertThat(page.getValue().getPageSize()).isEqualTo(500);
        assertThat(page.getValue().getPageNumber()).isZero();
    }
}
