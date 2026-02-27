package com.undercontroll.infrastructure.web.controller;

import com.undercontroll.domain.port.in.*;
import com.undercontroll.infrastructure.web.api.ComponentApi;
import com.undercontroll.application.dto.ComponentDto;
import com.undercontroll.infrastructure.web.dto.RegisterComponentRequest;
import com.undercontroll.infrastructure.web.dto.RegisterComponentResponse;
import com.undercontroll.infrastructure.web.dto.UpdateComponentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/api/components", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ComponentController implements ComponentApi {

    private final RegisterComponentPort registerComponentPort;
    private final GetComponentsPort getComponentsPort;
    private final GetComponentByIdPort getComponentByIdPort;
    private final GetComponentsByCategoryPort getComponentsByCategoryPort;
    private final GetComponentsByNamePort getComponentsByNamePort;
    private final UpdateComponentPort updateComponentPort;
    private final DeleteComponentPort deleteComponentPort;

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RegisterComponentResponse> register(@RequestBody RegisterComponentRequest request) {
        var response = registerComponentPort.execute(new RegisterComponentPort.Input(
                request.item(),
                request.description(),
                request.brand(),
                request.price(),
                request.supplier(),
                request.category(),
                request.quantity()
        ));
        return ResponseEntity.status(201).body(new RegisterComponentResponse(
                response.item(),
                response.description(),
                response.brand(),
                response.price(),
                response.supplier(),
                response.category()
        ));
    }

    @Override
    @GetMapping
    public ResponseEntity<List<ComponentDto>> findAll() {
        var response = getComponentsPort.execute();
        return response.components().isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(
                response.components().stream().map(
                        component -> new ComponentDto(
                                component.id(),
                                component.item(),
                                component.description(),
                                component.brand(),
                                component.price(),
                                component.quantity(),
                                component.supplier(),
                                component.category()
                        )
                )
                .toList()
        );
    }

    @Override
    @GetMapping("/{componentId}")
    public ResponseEntity<ComponentDto> getById(@PathVariable Integer componentId) {
        var response = getComponentByIdPort.execute(new GetComponentByIdPort.Input(componentId));
        return response.component() != null ? ResponseEntity.ok(response.component()) : ResponseEntity.notFound().build();
    }

    @Override
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ComponentDto>> findByCategory(@PathVariable String category) {
        var response = getComponentsByCategoryPort.execute(new GetComponentsByCategoryPort.Input(category));
        return response.components().isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(response.components());
    }

    @Override
    @GetMapping("/name/{name}")
    public ResponseEntity<List<ComponentDto>> findByName(@PathVariable String name) {
        var response = getComponentsByNamePort.execute(new GetComponentsByNamePort.Input(name));
        return response.components().isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(response.components());
    }

    @Override
    @PutMapping(value = "/{componentId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ComponentDto> updateComponent(@RequestBody UpdateComponentRequest request, @PathVariable Integer componentId) {
        var response = updateComponentPort.execute(new UpdateComponentPort.Input(
                componentId,
                request.item(),
                request.description(),
                request.brand(),
                request.price(),
                request.supplier(),
                request.category()
        ));
        return ResponseEntity.ok(new ComponentDto(
                response.id(),
                response.name(),
                response.description(),
                response.brand(),
                response.price(),
                response.quantity(),
                response.supplier(),
                response.category()
        ));
    }

    @Override
    @DeleteMapping("/{componentId}")
    public ResponseEntity<Void> deleteComponent(@PathVariable Integer componentId) {
        deleteComponentPort.execute(new DeleteComponentPort.Input(componentId));
        return ResponseEntity.ok().build();
    }
}
