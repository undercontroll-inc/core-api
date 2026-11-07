package com.undercontroll.domain.gateway;

import java.util.OptionalInt;

public interface PdfInspectorGateway {

    OptionalInt pageCount(byte[] pdf);
}
