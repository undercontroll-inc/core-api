package com.undercontroll.domain.model.stockimport;

public record InvoiceUpload(byte[] content, String contentType, String fileName) {

    public long size() {
        return content == null ? 0 : content.length;
    }
}
