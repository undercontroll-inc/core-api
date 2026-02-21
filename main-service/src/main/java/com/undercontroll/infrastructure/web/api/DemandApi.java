package com.undercontroll.infrastructure.web.api;

import com.undercontroll.infrastructure.config.ApiResponseDocumentation.*;
import com.undercontroll.infrastructure.web.dto.CreateDemandRequest;
import com.undercontroll.infrastructure.web.dto.DemandDto;
import com.undercontroll.domain.model.Demand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Demands", description = "APIs para gerenciamento de demandas de componentes em pedidos")
@SecurityRequirement(name = "Bearer Authentication")
public interface DemandApi {

    @Operation(summary = "Criar demanda")
    @PostApiResponses
    ResponseEntity<Demand> createDemand(CreateDemandRequest request);

    @Operation(summary = "Listar demandas de um pedido")
    @GetApiResponses
    ResponseEntity<List<DemandDto>> getDemandsByOrder(@Parameter(example = "5") Integer orderId);

    @Operation(summary = "Buscar demanda específica por pedido e componente")
    @GetApiResponses
    ResponseEntity<DemandDto> getDemandByOrderAndComponent(@Parameter(example = "5") Integer orderId, @Parameter(example = "10") Integer componentId);

    @Operation(summary = "Deletar demanda por ID")
    @DeleteApiResponses
    ResponseEntity<Void> deleteDemand(@Parameter(example = "25") Integer demandId);

    @Operation(summary = "Deletar todas demandas de um pedido")
    @DeleteApiResponses
    ResponseEntity<Void> deleteAllDemands(@Parameter(example = "5") Integer orderId);
}

