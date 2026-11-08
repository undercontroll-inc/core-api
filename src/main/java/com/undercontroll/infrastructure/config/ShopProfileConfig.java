package com.undercontroll.infrastructure.config;

import com.undercontroll.domain.model.ShopProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Configuration
public class ShopProfileConfig {

    @Bean
    ShopProfile shopProfile(
            @Value("${undercontroll.shop.name:Assistência Técnica}") String name,
            @Value("${undercontroll.shop.phone:}") String phone,
            @Value("${undercontroll.shop.whatsapp:}") String whatsapp,
            @Value("${undercontroll.shop.address:}") String address,
            @Value("${undercontroll.shop.hours:}") String hours,
            @Value("${undercontroll.shop.warranty-days:90}") int warrantyDays,
            @Value("${undercontroll.shop.zone:America/Sao_Paulo}") String zone,
            @Value("${undercontroll.public.base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        return new ShopProfile(name, phone, whatsapp, address, hours, warrantyDays, ZoneId.of(zone), publicBaseUrl);
    }
}
