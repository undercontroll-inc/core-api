package com.undercontroll.infrastructure.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.undercontroll.domain.enums.UserType;
import com.undercontroll.application.port.TokenPort;
import com.undercontroll.domain.exception.InvalidTokenException;
import com.undercontroll.domain.exception.TokenGenerationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@RequiredArgsConstructor
@Component
public class JwtTokenAdapter implements TokenPort {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration-minutes:15}")
    private long accessTokenExpirationMinutes;

    @Override
    public String generateToken(String username, UserType userType) {
        try{
            Algorithm algorithm = Algorithm.HMAC256(secret);

            return JWT.create()
                    .withIssuer("undercontroll")
                    .withClaim("roles", userType.name())
                    .withSubject(username)
                    .withExpiresAt(Instant.now().plusSeconds(accessTokenExpirationMinutes * 60))
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