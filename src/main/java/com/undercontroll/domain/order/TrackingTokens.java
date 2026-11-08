package com.undercontroll.domain.order;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.regex.Pattern;

public final class TrackingTokens {

    static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static final int LENGTH = 22;
    private static final BigInteger BASE = BigInteger.valueOf(ALPHABET.length());
    private static final Pattern VALID = Pattern.compile("^[0-9A-Za-z]{" + LENGTH + "}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private TrackingTokens() {
    }

    public static String generate() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        BigInteger value = new BigInteger(1, bytes);
        StringBuilder token = new StringBuilder(LENGTH);
        while (token.length() < LENGTH) {
            BigInteger[] division = value.divideAndRemainder(BASE);
            token.append(ALPHABET.charAt(division[1].intValue()));
            value = division[0];
        }
        return token.toString();
    }

    public static boolean wellFormed(String token) {
        return token != null && VALID.matcher(token).matches();
    }
}
