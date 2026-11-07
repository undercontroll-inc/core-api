package com.undercontroll.domain.model;

import com.undercontroll.domain.enums.UserType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@ToString
@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class User {

    private Integer id;

    private String name;

    private String lastName;

    private String email;

    private String password;

    private String address;

    private String cpf;

    private String CEP;

    private String phone;

    private Boolean alreadyRecurrent;

    private Boolean inFirstLogin;

    private String avatarUrl;

    private Boolean hasWhatsApp;

    private LocalDateTime createdAt;

    private UserType userType;

    public String fullName() {
        String joined = String.join(" ", Objects.toString(name, ""), Objects.toString(lastName, "")).trim();
        return joined.isBlank() ? null : joined;
    }
}
