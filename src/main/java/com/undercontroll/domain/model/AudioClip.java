package com.undercontroll.domain.model;

public record AudioClip(byte[] content, String contentType, String filename, AudioFormat format) {

    public AudioClip(byte[] content, String contentType, String filename) {
        this(content, contentType, filename, null);
    }

    public long size() {
        return content == null ? 0L : content.length;
    }

    public AudioClip normalized(AudioFormat detected) {
        return new AudioClip(content, detected.mimeType(), baseName() + "." + detected.extension(), detected);
    }

    private String baseName() {
        if (filename == null || filename.isBlank()) {
            return "audio";
        }
        String name = filename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        return base.isBlank() ? "audio" : base;
    }
}
