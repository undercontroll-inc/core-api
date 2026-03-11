package com.undercontroll.infrastructure.persistence.entity;

import com.undercontroll.domain.model.User;
import com.undercontroll.domain.enums.UserType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;

@ToString
@Entity
@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Table(name = "`user`")
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private String lastName;

    private String email;

    private String password;

    private String address;

    private String cpf;

    @Length(min = 8, max = 8)
    private String CEP;

    private String phone;

    private Boolean alreadyRecurrent;

    private Boolean inFirstLogin;

    private String avatarUrl;

    private Boolean hasWhatsApp;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private UserType userType;

    public User toDomain() {
        return User.builder()
                .id(id)
                .name(name)
                .lastName(lastName)
                .email(email)
                .password(password)
                .address(address)
                .cpf(cpf)
                .CEP(CEP)
                .phone(phone)
                .alreadyRecurrent(alreadyRecurrent)
                .inFirstLogin(inFirstLogin)
                .avatarUrl(avatarUrl)
                .hasWhatsApp(hasWhatsApp)
                .createdAt(createdAt)
                .userType(userType)
                .build();
    }

    public static UserJpaEntity fromDomain(User user) {
        if (user == null) return null;
        return UserJpaEntity.builder()
                .id(user.getId())
                .name(user.getName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .password(user.getPassword())
                .address(user.getAddress())
                .cpf(user.getCpf())
                .CEP(user.getCEP())
                .phone(user.getPhone())
                .alreadyRecurrent(user.getAlreadyRecurrent())
                .inFirstLogin(user.getInFirstLogin())
                .avatarUrl(user.getAvatarUrl())
                .hasWhatsApp(user.getHasWhatsApp())
                .createdAt(user.getCreatedAt())
                .userType(user.getUserType())
                .build();
    }

}
