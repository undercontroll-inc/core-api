package com.undercontroll.infrastructure.web.dto;

import java.util.List;

public record ExportOrderRequest(
        String numeroOs,
        String os,
        String nf,
        String data,
        String loja,

        List<ProductInfo> produtos,

        String nome,
        String endereco,
        String telefone,
        String recepcao,

        List<PartInfo> pecas,
        String total,

        String dataConserto,
        String tecnico,

        Boolean garantiaFabrica,
        Boolean orcamento,
        Boolean retornoGarantia
) {

    public record ProductInfo(
            String produto,
            String volt,
            String serie
    ) {}

    public record PartInfo(
            Long quantidade,
            String peca,
            String valor
    ) {}
}
