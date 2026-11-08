package com.undercontroll.domain.exception;

public class LabelReadingUnavailableException extends RuntimeException {

    public static final String CODE = "LABEL_READING_UNAVAILABLE";

    public LabelReadingUnavailableException() {
        super("A leitura de etiqueta está indisponível no momento. Preencha os dados manualmente.");
    }

    public LabelReadingUnavailableException(Throwable cause) {
        super("A leitura de etiqueta está indisponível no momento. Preencha os dados manualmente.", cause);
    }
}
