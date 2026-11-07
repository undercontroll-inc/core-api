package com.undercontroll.domain.model.stockimport;

import com.undercontroll.domain.enums.StockImportSourceType;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum InvoiceFileType {
    JPEG("image/jpeg", StockImportSourceType.IMAGE, Set.of("image/jpeg", "image/jpg", "image/pjpeg")),
    PNG("image/png", StockImportSourceType.IMAGE, Set.of("image/png")),
    WEBP("image/webp", StockImportSourceType.IMAGE, Set.of("image/webp")),
    PDF("application/pdf", StockImportSourceType.PDF, Set.of("application/pdf")),
    XML("application/xml", StockImportSourceType.NFE_XML, Set.of("application/xml", "text/xml"));

    private final String mimeType;
    private final StockImportSourceType sourceType;
    private final Set<String> aliases;

    InvoiceFileType(String mimeType, StockImportSourceType sourceType, Set<String> aliases) {
        this.mimeType = mimeType;
        this.sourceType = sourceType;
        this.aliases = aliases;
    }

    public String mimeType() {
        return mimeType;
    }

    public StockImportSourceType sourceType() {
        return sourceType;
    }

    public static Optional<InvoiceFileType> fromMimeType(String mimeType) {
        if (mimeType == null) {
            return Optional.empty();
        }
        String normalized = mimeType.toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(type -> type.aliases.contains(normalized)).findFirst();
    }

    public static Optional<InvoiceFileType> detect(byte[] content) {
        if (content == null || content.length < 4) {
            return Optional.empty();
        }
        if ((content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8 && (content[2] & 0xFF) == 0xFF) {
            return Optional.of(JPEG);
        }
        if ((content[0] & 0xFF) == 0x89 && content[1] == 'P' && content[2] == 'N' && content[3] == 'G') {
            return Optional.of(PNG);
        }
        if (content.length >= 12 && ascii(content, 0, 4).equals("RIFF") && ascii(content, 8, 4).equals("WEBP")) {
            return Optional.of(WEBP);
        }
        if (ascii(content, 0, Math.min(content.length, 1024)).contains("%PDF-")) {
            return Optional.of(PDF);
        }
        if (looksLikeXml(content)) {
            return Optional.of(XML);
        }
        return Optional.empty();
    }

    private static boolean looksLikeXml(byte[] content) {
        int start = 0;
        if (content.length >= 3 && (content[0] & 0xFF) == 0xEF && (content[1] & 0xFF) == 0xBB && (content[2] & 0xFF) == 0xBF) {
            start = 3;
        }
        while (start < content.length && Character.isWhitespace(content[start])) {
            start++;
        }
        return start < content.length && content[start] == '<';
    }

    private static String ascii(byte[] content, int offset, int length) {
        return new String(content, offset, Math.min(length, content.length - offset), StandardCharsets.ISO_8859_1);
    }
}
