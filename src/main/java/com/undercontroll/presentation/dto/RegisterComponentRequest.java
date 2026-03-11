package com.undercontroll.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Requisição para criação de novo componente")
public record RegisterComponentRequest(
        @Schema(description = "Nome do componente", example = "Resistor 10k Ohm")
        String item,

        @Schema(description = "Descrição detalhada do componente", example = "Resistor de precisão 10k Ohm 1% 1/4W")
        String description,

        @Schema(description = "Marca/fabricante do componente", example = "Vishay")
        String brand,

        @Schema(description = "Categoria do componente", example = "Electronics")
        String category,

        @Schema(description = "Quantidade em estoque", example = "100")
        Integer quantity,

        @Schema(description = "Preço unitário do componente", example = "1.50")
        Double price,

        @Schema(description = "Fornecedor do componente", example = "Mouser Electronics")
        String supplier
) {
}
