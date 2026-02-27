package com.undercontroll.infrastructure.adapter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.undercontroll.domain.model.enums.UserType;
import com.undercontroll.domain.port.out.TokenPort;
import com.undercontroll.domain.exception.InvalidTokenException;
import com.undercontroll.domain.exception.TokenGenerationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@RequiredArgsConstructor
@Component
public class JwtTokenPort implements TokenPort {

    private static final Long TOKEN_EXPIRATION_SECONDS = 15L * 60L; // 15 minutos

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public String generateToken(String username, UserType userType) {
        try{
            Algorithm algorithm = Algorithm.HMAC256(secret);

            return JWT.create()
                    .withIssuer("undercontroll")
                    .withClaim("roles", userType.name())
                    .withSubject(username)
                    .withExpiresAt(Instant.now().plusSeconds(TOKEN_EXPIRATION_SECONDS))
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new TokenGenerationException("Error while generating token " + exception.getMessage());
        }
    }

    @Override
    public DecodedJWT validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            return JWT.require(algorithm)
                    .withIssuer("undercontroll")
                    .build()
                    .verify(token);
        } catch (Exception e) {
            throw new InvalidTokenException("Error while validating token " + e.getMessage());
        }
    }

    @Override
    public String extractUsername(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            return JWT.require(algorithm)
                    .withIssuer("undercontroll")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception){
            throw new InvalidTokenException("Received a invalid jwt token " + exception.getMessage());
        }
    }
}