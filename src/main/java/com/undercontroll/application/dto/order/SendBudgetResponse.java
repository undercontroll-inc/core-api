package com.undercontroll.application.dto.order;

public record SendBudgetResponse(String trackingUrl, String whatsappText, OrderEnrichedDto order) {
}
