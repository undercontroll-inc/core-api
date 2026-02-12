package com.undercontroll.infrastructure.web.controller;

import com.undercontroll.infrastructure.web.api.OrderApi;
import com.undercontroll.infrastructure.web.dto.*;
import com.undercontroll.domain.entity.Order;
import com.undercontroll.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "/v1/api/orders", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class OrderController implements OrderApi {

    private final OrderService orderService;

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest createOrderRequest) {
        Order order = orderService.createOrder(createOrderRequest);
        return ResponseEntity.status(201).body(order);
    }

    @Override
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateOrder(@PathVariable Integer id, @RequestBody UpdateOrderRequest request) {
        orderService.updateOrder(request, id);
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping
    public ResponseEntity<GetAllOrdersResponse> getOrders() {
        GetAllOrdersResponse orders = orderService.getOrders();
        return ResponseEntity.ok(orders);
    }

    @Override
    @GetMapping("/{orderId}")
    public ResponseEntity<GetOrderByIdResponse> getOrderById(@PathVariable Integer orderId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = null;
        if (auth != null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof Jwt jwt) {
                email = jwt.getClaimAsString("sub");
            }
        }
        if (email == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        GetOrderByIdResponse response = orderService.getOrderById(orderId, email);
        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Integer orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/filter")
    public ResponseEntity<GetOrdersByUserIdResponse> getOrdersByUserId(@RequestParam("userId") Integer userId) {
        log.info("Oie");
        GetOrdersByUserIdResponse response = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export/{orderId}")
    public ResponseEntity<byte[]> exportOrder(@PathVariable Integer orderId) {
        byte[] pdf = orderService.exportPdf(orderId);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"relatorio.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}

