package com.undercontroll.domain.model;

import com.undercontroll.domain.enums.UserType;
import lombok.*;

import java.time.LocalDateTime;

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

}
