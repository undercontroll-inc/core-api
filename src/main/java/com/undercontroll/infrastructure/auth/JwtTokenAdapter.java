package com.undercontroll.infrastructure.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.undercontroll.domain.model.AccessTokenClaims;
import com.undercontroll.domain.enums.UserType;
import com.undercontroll.domain.gateway.TokenGateway;
import com.undercontroll.domain.exception.InvalidTokenException;
import com.undercontroll.domain.exception.TokenGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtTokenAdapter implements TokenGateway {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration-minutes:15}")
    private long accessTokenExpirationMinutes;

    @Override
    public String generateToken(String userId, UserType userType) {
        try{
            Algorithm algorithm = Algorithm.HMAC256(secret);

            return JWT.create()
                    .withIssuer("undercontroll")
                    .withClaim("roles", userType.name())
                    .withSubject(userId)
                    .withExpiresAt(Instant.now().plusSeconds(accessTokenExpirationMinutes * 60))
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            log.error("JWT generation failed");
            throw new TokenGenerationException("Error while generating token " + exception.getMessage());
        }
    }

    @Override
    public AccessTokenClaims validateToken(String token) {
        DecodedJWT decoded = verify(token);
        return new AccessTokenClaims(decoded.getSubject(), decoded.getClaim("roles").asString());
    }

    private DecodedJWT verify(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Invalid token");
        }

        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            return JWT.require(algorithm)
                    .withIssuer("undercontroll")
                    .build()
                    .verify(token);
        } catch (TokenExpiredException e) {
            log.debug("Access token expired");
            throw new InvalidTokenException("Access token has expired", InvalidTokenException.TOKEN_EXPIRED);
        } catch (JWTVerificationException e) {
            log.debug("Access token invalid");
            throw new InvalidTokenException("Invalid token");
        }
    }
}
