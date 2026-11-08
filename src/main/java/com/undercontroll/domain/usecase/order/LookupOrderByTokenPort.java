package com.undercontroll.domain.usecase.order;

public interface LookupOrderByTokenPort {

    Integer execute(String token);
}
