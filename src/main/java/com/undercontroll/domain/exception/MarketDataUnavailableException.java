package com.undercontroll.domain.exception;

public class MarketDataUnavailableException extends RuntimeException {

    public static final String CODE = "MARKET_DATA_UNAVAILABLE";
    public static final String MESSAGE = "Market data is temporarily unavailable.";

    public MarketDataUnavailableException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
