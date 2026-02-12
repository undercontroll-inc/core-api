package com.undercontroll.application.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleTokenVerifier {

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
                log.warn("Email mismatch. Expected: {}, Got: {}", expectedEmail, email);
                return false;
            }

            if (!emailVerified) {
                log.warn("Email is not verified for user: {}", email);
                return false;
            }

            log.info("Token verified successfully for user: {}", email);
            return true;

        } catch (FirebaseAuthException e) {
            log.error("Failed to verify Firebase token: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during token verification", e);
            return false;
        }
    }
}

