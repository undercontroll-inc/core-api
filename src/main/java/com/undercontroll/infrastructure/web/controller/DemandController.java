package com.undercontroll.infrastructure.web.controller;

import com.undercontroll.domain.port.in.*;
import com.undercontroll.infrastructure.web.api.DemandApi;
import com.undercontroll.infrastructure.web.dto.CreateDemandRequest;
import com.undercontroll.application.dto.DemandDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/api/demands", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class DemandController implements DemandApi {

    private final CreateDemandPort createDemandPort;
    private final GetDemandsPort getDemandsPort;
    private final GetDemandByOrderAndComponentPort getDemandByOrderAndComponentPort;
    private final DeleteDemandPort deleteDemandPort;
    private final DeleteAllDemandsByOrderPort deleteAllDemandsByOrderPort;

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DemandDto> createDemand(@RequestBody CreateDemandRequest request) {
        var output = createDemandPort.execute(new CreateDemandPort.Input(
                request.componentPartId(), request.quantity(), request.orderId()
        ));
        return ResponseEntity.status(201).body(new DemandDto(
                output.id(), output.componentId(), output.orderId(), output.quantity()
        ));
    }

    @Override
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<DemandDto>> getDemandsByOrder(@PathVariable Integer orderId) {
        var output = getDemandsPort.execute(new GetDemandsPort.Input(orderId));
        return output.demands().isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(output.demands());
    }

    @Override
    @GetMapping("/order/{orderId}/component/{componentId}")
    public ResponseEntity<DemandDto> getDemandByOrderAndComponent(@PathVariable Integer orderId, @PathVariable Integer componentId) {
        var output = getDemandByOrderAndComponentPort.execute(new GetDemandByOrderAndComponentPort.Input(orderId, componentId));
        return output.demand() != null ? ResponseEntity.ok(output.demand()) : ResponseEntity.notFound().build();
    }

    @Override
    @DeleteMapping("/{demandId}")
    public ResponseEntity<Void> deleteDemand(@PathVariable Integer demandId) {
        deleteDemandPort.execute(new DeleteDemandPort.Input(demandId));
        return ResponseEntity.ok().build();
    }

    @Override
    @DeleteMapping("/order/{orderId}")
    public ResponseEntity<Void> deleteAllDemands(@PathVariable Integer orderId) {
        deleteAllDemandsByOrderPort.execute(new DeleteAllDemandsByOrderPort.Input(orderId));
        return ResponseEntity.ok().build();
    }
}
