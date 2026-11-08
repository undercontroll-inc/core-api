package com.undercontroll.domain.exception;

public class OrderBudgetEmptyException extends RuntimeException {

    public static final String CODE = "ORDER_BUDGET_EMPTY";

    public OrderBudgetEmptyException() {
        super("O orçamento precisa ter valor maior que zero antes de ser enviado");
    }
}
