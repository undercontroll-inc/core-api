package com.undercontroll.infrastructure.web.api;

import com.undercontroll.infrastructure.config.swagger.ApiResponseDocumentation.*;
import com.undercontroll.infrastructure.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Components", description = "APIs para gerenciamento de componentes eletrônicos e materiais")
@SecurityRequirement(name = "Bearer Authentication")
public interface ComponentApi {

    @Operation(summary = "Criar novo componente")
    @PostApiResponses
    ResponseEntity<RegisterComponentResponse> register(RegisterComponentRequest request);

    @Operation(summary = "Listar todos os componentes")
    @GetApiResponses
    ResponseEntity<List<ComponentDto>> findAll();

    @Operation(summary = "Buscar componente por ID")
    @GetApiResponses
    ResponseEntity<ComponentDto> getById(@Parameter(example = "1") Integer componentId);

    @Operation(summary = "Buscar componentes por categoria")
    @GetApiResponses
    ResponseEntity<List<ComponentDto>> findByCategory(@Parameter(example = "Electronics") String category);

    @Operation(summary = "Buscar componentes por nome")
    @GetApiResponses
    ResponseEntity<List<ComponentDto>> findByName(@Parameter(example = "Resistor") String name);

    @Operation(summary = "Atualizar componente")
    @PutApiResponses
    ResponseEntity<ComponentDto> updateComponent(UpdateComponentRequest request, @Parameter(example = "1") Integer componentId);

    @Operation(summary = "Deletar componente")
    @DeleteApiResponses
    ResponseEntity<Void> deleteComponent(@Parameter(example = "1") Integer componentId);
}

