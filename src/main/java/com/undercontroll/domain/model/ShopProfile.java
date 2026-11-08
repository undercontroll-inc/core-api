package com.undercontroll.domain.model;

import java.time.ZoneId;

public record ShopProfile(
        String name,
        String phone,
        String whatsapp,
        String address,
        String hours,
        int warrantyDays,
        ZoneId zone,
        String publicBaseUrl
) {

    public String trackingUrl(String token) {
        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        return base + "/p/" + token;
    }

    public String phoneDigits() {
        return phone == null ? "" : phone.replaceAll("\\D", "");
    }

    public String whatsappDigits() {
        String source = whatsapp == null || whatsapp.isBlank() ? phone : whatsapp;
        return source == null ? "" : source.replaceAll("\\D", "");
    }
}
