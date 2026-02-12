package com.undercontroll.application.service;

import com.undercontroll.domain.exception.*;
import com.undercontroll.infrastructure.web.dto.*;
import com.undercontroll.domain.entity.User;
import com.undercontroll.domain.entity.enums.PasswordEventType;
import com.undercontroll.domain.entity.enums.UserType;
import com.undercontroll.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

@Validated
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserJpaRepository repository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordEventService passwordEventService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final MetricsService metricsService;

    @CacheEvict(value = {"users", "customers"}, allEntries = true)
    public CreateUserResponse createUser(
            CreateUserRequest request
    ) {
        try {
            validateCreateUserRequest(request);

            Optional<User> existingUserByEmail = repository.findUserByEmail(request.email());

            if(existingUserByEmail.isPresent()) {
                throw new InvalidUserException("Email is already in use");
            }

            Optional<User> existingUserByPhone = repository.findUserByPhone(request.phone());

            if (existingUserByPhone.isPresent()) {
                throw new InvalidUserException("Phone is already in use");
            }

            Optional<User> existingUserByCpf = repository.findUserByCpf(request.cpf());

            if(existingUserByCpf.isPresent()) {
                throw new InvalidUserException("CPF is already in use");
            }

            String password =
                    request.userType().equals(UserType.ADMINISTRATOR)
                    ? passwordEncoder.encode(request.password())
                    : passwordEncoder.encode(passwordEventService.create(new CreatePasswordEventRequest(
                            PasswordEventType.CREATE,
                            null,
                            request.phone(),
                            null // Passando nulo pois se trata de um evento de criacao de senha, nao de reset.
                    )).getValue());

            User user = User.builder()
                    .name(request.name())
                    .email(request.email())
                    .lastName(request.lastName())
                    .password(password)
                    .address(request.address())
                    .cpf(request.cpf())
                    .CEP(request.CEP())
                    .phone(request.phone())
                    .avatarUrl(request.avatarUrl())
                    .hasWhatsApp(request.hasWhatsApp())
                    .alreadyRecurrent(request.alreadyRecurrent())
                    .inFirstLogin(request.inFirstLogin())
                    .userType(request.userType())
                    .build();

            repository.save(user);

            metricsService.incrementAccountCreated();

            return new CreateUserResponse(
                    request.name(),
                    request.email(),
                    request.lastName(),
                    request.address(),
                    request.cpf(),
                    request.CEP(),
                    request.phone(),
                    request.avatarUrl(),
                    request.userType()
            );
        } catch (InvalidUserException e) {
            metricsService.incrementAccountCreationFailed();
            throw e;
        }
    }

    public AuthUserResponse authUser(AuthUserRequest request) {
        try {
            Optional<User> userFound = repository.findUserByEmail(request.email());

            if(userFound.isEmpty()){
                metricsService.incrementLoginFailed();
                throw new InvalidAuthException("Email or password is invalid");
            }

            boolean passwordMatch = passwordEncoder
                    .matches(request.password(), userFound.get().getPassword());

            if(!passwordMatch){
                metricsService.incrementLoginFailed();
                throw new InvalidAuthException("Email or password is invalid");
            }

            User user = userFound.get();
            String token = tokenService.generateToken(user.getEmail(), user.getUserType());

            metricsService.incrementLoginSuccess();

            return new AuthUserResponse(
                    token,
                    mapToDto(user)
            );
        } catch (InvalidAuthException e) {
            throw e;
        }
    }

    @CacheEvict(value = {"users", "customers", "user"}, allEntries = true)
    public void updateUser (UpdateUserRequest request, Integer id) {
        validateUpdateUser(request);

        Optional<User> user = repository.findById(id);

        if(user.isEmpty()) {
            throw new InvalidUserException("Could not found the user for update with id: %d".formatted(id));
        }

        User userFound = user.get();

        if (request.name() != null) {
            userFound.setName(request.name());
        }
        if (request.lastName() != null) {
            userFound.setLastName(request.lastName());
        }
        if (request.address() != null) {
            userFound.setAddress(request.address());
        }
        if (request.userType() != null) {
            userFound.setUserType(request.userType());
        }
        if (request.cpf() != null) {
            userFound.setCpf(request.cpf());
        }
        if (request.password() != null) {
            userFound.setPassword(request.password());
        }
        if(request.hasWhatsApp() != null) {
            userFound.setHasWhatsApp(request.hasWhatsApp());
        }
        if(request.CEP() != null){
            userFound.setCEP(request.CEP());
        }
        if(request.alreadyRecurrent() != null) {
            userFound.setAlreadyRecurrent(request.alreadyRecurrent());
        }
        if(request.inFirstLogin() != null) {
            userFound.setInFirstLogin(request.inFirstLogin());
        }
        if(request.phone() != null) {
            userFound.setPhone(request.phone());
        }

        if(request.avatarUrl() != null) {
            userFound.setAvatarUrl(request.avatarUrl());
        }

        repository.save(userFound);
    }

    @Cacheable(value = "users")
    public List<UserDto> getUsers() {
        return repository
                .findAll()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @CacheEvict(value = {"users", "customers", "user"}, allEntries = true)
    public void deleteUser(Integer userId) {
        if (userId == null) {
            throw new InvalidUserException("User ID cannot be null");
        }

        Optional<User> user = repository.findById(userId);

        if(user.isEmpty()) {
            throw  new InvalidUserException("Could not found the user with id: %d".formatted(userId));
        }
    }

    public AuthUserResponse authUserByGoogle(AuthGoogleRequest request) {
        try {
            Optional<User> userFound = repository.findUserByEmail(request.email());

            if(userFound.isEmpty()){
                metricsService.incrementGoogleLoginFailed();
                throw new GoogleAccountNotFoundException();
            }

            boolean valid = googleTokenVerifier.verify(request.token(), request.email());

            if(!valid){
                metricsService.incrementGoogleLoginFailed();
                throw new InvalidAuthException("Google token is invalid");
            }

            User user = userFound.get();
            String token = tokenService.generateToken(user.getEmail(), user.getUserType());

            metricsService.incrementGoogleLoginSuccess();

            return new AuthUserResponse(
                    token,
                    mapToDto(user)
            );
        } catch (GoogleAccountNotFoundException | InvalidAuthException e) {
            throw e;
        }
    }

    @Cacheable(value = "user", key = "#userId")
    public User getUserById(Integer userId) {
        if (userId == null) {
            throw new InvalidUserException("User ID cannot be null");
        }

        Optional<User> user = repository.findById(userId);

        if(user.isEmpty()) {
            throw  new InvalidUserException("Could not found the user with id: %d".formatted(userId));
        }

        return user.get();
    }

    @Cacheable(value = "user", key = "#token")
    public User getUserByToken(String token) {
        String subject = tokenService.extractUsername(token);

        if(subject == null || subject.isEmpty()) {
            throw new InvalidAuthException("Token is invalid");
        }

        Optional<User> user = repository.findUserByEmail(subject);

        if(user.isEmpty()) {
            throw new UserNotFoundException("User not found with email: %s".formatted(subject));
        }

        return user.get();
    }

    @Cacheable(value = "user", key = "#email")
    public User getUserByEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new InvalidAuthException("Email cannot be null or empty");
        }

        Optional<User> user = repository.findUserByEmail(email);

        if (user.isEmpty()) {
            throw new UserNotFoundException("User not found with email: %s".formatted(email));
        }

        return user.get();
    }

    @Cacheable(value = "customers")
    public List<UserDto> getCustomers() {
        return this.repository
                .findAllCustomers()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Cacheable(value = "user", key = "#id")
    public UserDto getCustomersById(Integer id) {
        return this.repository
                .findCustomerById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new UserNotFoundException("Costumer not found with id: %d".formatted(id)));
    }

    @Cacheable(value = "customers")
    public List<UserDto> findAllCustomersThatHaveEmail() {
        return this
                .repository
                .findAllCustomersThatHaveEmail()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public void resetPassword(
            ResetPasswordRequest request,
            Integer userId,
            String token
    ) {
        if(request.newPassword().isEmpty()) {
            throw new InvalidPasswordResetException("New password cannot be empty");
        }

        Optional<User> user = repository.findById(userId);

        if(user.isEmpty()) {
            throw new UserNotFoundException("");
        }

        // Validacao se o usuario esta tentando alterar a senha a senha de outra conta.
        if(!tokenService.extractUsername(token).equals(user.get().getEmail())) {
            throw new InvalidAuthException("Cannot change the password, of another account.");
        }

        String encryptedPassword = passwordEncoder.encode(request.newPassword());

        passwordEventService.create(new CreatePasswordEventRequest(
                PasswordEventType.RESET,
                null,
                user.get().getPhone(),
                encryptedPassword
        ));

        if(request.inFirstLogin()) {
            user.get().setInFirstLogin(false);
        }

        user.get().setPassword(encryptedPassword);

        repository.save(user.get());
    }


    private void validateCreateUserRequest(CreateUserRequest request) {
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new InvalidUserException("User name cannot be empty");
        }

        if(request.CEP() == null || request.CEP().isEmpty()){
            throw new InvalidUserException("CEP cannot be empty");
        }

        if(request.phone() == null || request.phone().isEmpty()){
            throw new InvalidUserException("Phone number cannot be empty");
        }

        if (request.address() == null || request.address().trim().isEmpty()) {
            throw new InvalidUserException("User address cannot be empty");
        }

        if (request.lastName() == null || request.lastName().trim().isEmpty()) {
            throw new InvalidUserException("User last name cannot be empty");
        }

        if (request.password() == null || request.password().trim().isEmpty()) {
            throw new InvalidUserException("User password cannot be empty");
        }
    }

    private void validateUpdateUser(UpdateUserRequest request) {
        // Validação pode ser adicionada no futuro se necessário
    }

    public UserDto mapToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getLastName(),
                user.getAddress(),
                user.getCpf(),
                user.getCEP(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getHasWhatsApp(),
                user.getAlreadyRecurrent(),
                user.getInFirstLogin(),
                user.getUserType()
        );
    }
}