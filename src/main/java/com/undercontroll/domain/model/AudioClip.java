package com.undercontroll.domain.model;

public record AudioClip(byte[] content, String contentType, String filename) {

    public long size() {
        return content == null ? 0L : content.length;
    }
}
