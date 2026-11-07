package com.undercontroll.infrastructure.auth;

import com.undercontroll.domain.gateway.GoogleAuthGateway;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleTokenVerifierAdapter implements GoogleAuthGateway {

    @Override
    public boolean verify(String idToken, String expectedEmail) {
        if (idToken == null || idToken.isBlank()) {
            log.warn("Token is null or blank");
            return false;
        }

        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);

            String email = decodedToken.getEmail();
            boolean emailVerified = decodedToken.isEmailVerified();

            if (email == null) {
                log.warn("Token does not contain email");
                return false;
            }

            if (!email.equalsIgnoreCase(expectedEmail)) {
                log.warn("Google token email mismatch");
                return false;
            }

            if (!emailVerified) {
                log.warn("Google token email is not verified");
                return false;
            }

            log.debug("Google token verified");
            return true;

        } catch (FirebaseAuthException e) {
            log.warn("Failed to verify Firebase token: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during token verification", e);
            return false;
        }
    }
}
