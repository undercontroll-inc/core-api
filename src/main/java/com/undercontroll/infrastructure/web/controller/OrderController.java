package com.undercontroll.infrastructure.web.controller;

import com.undercontroll.domain.port.in.*;
import com.undercontroll.infrastructure.web.api.OrderApi;
import com.undercontroll.application.dto.*;
import com.undercontroll.infrastructure.web.dto.*;
import com.undercontroll.domain.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/v1/api/orders", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class OrderController implements OrderApi {

    private final CreateOrderPort createOrderPort;
    private final UpdateOrderPort updateOrderPort;
    private final GetOrdersPort getOrdersPort;
    private final GetOrderByIdPort getOrderByIdPort;
    private final DeleteOrderPort deleteOrderPort;
    private final GetOrdersByUserIdPort getOrdersByUserIdPort;
    private final ExportOrderPort exportOrderPort;

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        var output = createOrderPort.execute(new CreateOrderPort.Input(
                request.userId(),
                request.parts(),
                request.appliances(),
                request.discount(),
                request.deadline(),
                request.receivedAt(),
                request.nf(),
                request.fabricGuarantee(),
                request.returnGuarantee(),
                request.serviceDescription()
        ));
        return ResponseEntity.status(201).body(new Order());
    }

    @Override
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateOrder(@PathVariable Integer id, @RequestBody UpdateOrderRequest request) {
        updateOrderPort.execute(new UpdateOrderPort.Input(
                id,
                request.status(),
                request.parts(),
                request.appliances(),
                request.serviceDescription()
        ));
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping
    public ResponseEntity<GetAllOrdersResponse> getOrders() {
        var output = getOrdersPort.execute(new GetOrdersPort.Input());
        return ResponseEntity.ok(new GetAllOrdersResponse(output.orders()));
    }

    @Override
    @GetMapping("/{orderId}")
    public ResponseEntity<GetOrderByIdResponse> getOrderById(@PathVariable Integer orderId) {
        var output = getOrderByIdPort.execute(new GetOrderByIdPort.Input(orderId, null));
        return output.order() != null ? ResponseEntity.ok(output.order()) : ResponseEntity.notFound().build();
    }

    @Override
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Integer orderId) {
        deleteOrderPort.execute(new DeleteOrderPort.Input(orderId));
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/filter")
    public ResponseEntity<GetOrdersByUserIdResponse> getOrdersByUserId(@RequestParam("userId") Integer userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATOR"));

        Integer authenticatedUserId = Integer.parseInt(auth.getName());

        if (!isAdmin && !authenticatedUserId.equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        var output = getOrdersByUserIdPort.execute(new GetOrdersByUserIdPort.Input(userId));
        return ResponseEntity.ok(output.orders());
    }

    @GetMapping("/export/{orderId}")
    public ResponseEntity<byte[]> exportOrder(@PathVariable Integer orderId) {
        var output = exportOrderPort.execute(new ExportOrderPort.Input(orderId));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"relatorio.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(output.pdfData());
    }
}
