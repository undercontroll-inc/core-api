package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.domain.entity.Order;
import com.undercontroll.domain.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderJpaRepository extends JpaRepository<Order, Integer> {

    List<Order> findByUser_id(Integer userId);


    @Query("SELECT o FROM Order o JOIN o.orderItems oi WHERE oi.id = :orderItemId")
    Optional<Order> findOrderByOrderItemId(@Param("orderItemId") Integer orderItemId);

    // Query responsavel por retornar o total de todos os componentes de todos os items relacionados a um pedido
    @Query(value = """
    SELECT COALESCE(SUM(c.price * d.quantity), 0.0)
    FROM `order` o
    INNER JOIN demand d ON o.id = d.order_id
    INNER JOIN component c ON d.component_id = c.id
    WHERE o.id = :orderId
    """, nativeQuery = true)
    Double calculatePartsTotalByOrderId(@Param("orderId") Integer orderId);

    // Query responsavel por retornar a lista de peças utilizadas em todos os items de um pedido específico
    @Query(value = """
    SELECT
        c.id,
        c.name,
        c.description,
        c.brand,
        c.price,
        c.supplier,
        c.category,
        d.quantity
    FROM `order` o
    INNER JOIN demand d ON o.id = d.order_id
    INNER JOIN component c ON d.component_id = c.id
    WHERE o.id = :orderId
    """, nativeQuery = true)
    List<Object[]> findAllPartsByOrderIdNative(@Param("orderId") Integer orderId);


    // Calculate total revenue with filters
    @Query("SELECT COALESCE(SUM(o.total), 0.0) FROM Order o " +
           "WHERE (:startDate IS NULL OR o.received_at >= :startDate) " +
           "AND o.status IN :statuses")
    Double calculateTotalRevenueFiltered(
            @Param("startDate") LocalDate startDate,
            @Param("statuses") List<OrderStatus> statuses);

    // Calculate total parts cost with filters
    @Query(value = """
    SELECT COALESCE(SUM(c.price * d.quantity), 0.0)
    FROM `order` o
    INNER JOIN demand d ON o.id = d.order_id
    INNER JOIN component c ON d.component_id = c.id
    WHERE (:startDate IS NULL OR o.received_at >= :startDate)
      AND o.status IN :statuses
    """, nativeQuery = true)
    Double calculateTotalPartsCostFiltered(
            @Param("startDate") LocalDate startDate,
            @Param("statuses") List<String> statuses);

    // Calculate average order price with filters
    @Query("SELECT COALESCE(AVG(o.total), 0.0) FROM Order o " +
           "WHERE (:startDate IS NULL OR o.received_at >= :startDate) " +
           "AND o.status IN :statuses")
    Double calculateAverageOrderPriceFiltered(
            @Param("startDate") LocalDate startDate,
            @Param("statuses") List<OrderStatus> statuses);

    // Count ongoing orders with filters
    @Query("SELECT COUNT(o) FROM Order o " +
           "WHERE (:startDate IS NULL OR o.received_at >= :startDate) " +
           "AND o.status IN :statuses")
    Long countOngoingOrdersFiltered(
            @Param("startDate") LocalDate startDate,
            @Param("statuses") List<OrderStatus> statuses);

    // Calculate average repair time with filters
    @Query(value = """
    SELECT COALESCE(AVG(TIMESTAMPDIFF(HOUR, received_at, completed_time)), 0.0)
    FROM `order`
    WHERE (:startDate IS NULL OR received_at >= :startDate)
      AND status IN :statuses
      AND received_at IS NOT NULL
      AND completed_time IS NOT NULL
    """, nativeQuery = true)
    Double calculateAverageRepairTimeFiltered(
            @Param("startDate") LocalDate startDate,
            @Param("statuses") List<String> statuses);


    // Revenue Evolution - Daily aggregation
    @Query(value = """
    SELECT
        o.received_at as date,
        COALESCE(SUM(o.total), 0.0) as revenue,
        COALESCE(SUM(o.total) - SUM(COALESCE(parts_cost, 0)), 0.0) as profit,
        COUNT(o.id) as order_count
    FROM `order` o
    LEFT JOIN (
        SELECT d.order_id, SUM(c.price * d.quantity) as parts_cost
        FROM demand d
        INNER JOIN component c ON d.component_id = c.id
        GROUP BY d.order_id
    ) parts ON o.id = parts.order_id
    WHERE (:startDate IS NULL OR o.received_at >= :startDate)
      AND o.status IN :statuses
      AND o.received_at IS NOT NULL
    GROUP BY o.received_at
    ORDER BY date
    """, nativeQuery = true)
    List<Object[]> getRevenueEvolution(
            @Param("startDate") LocalDate startDate,
            @Param("statuses") List<String> statuses);

    // Customer Type Evolution - Daily aggregation
    @Query(value = """
    SELECT
        o.received_at as date,
        COUNT(DISTINCT CASE WHEN u.already_recurrent = true THEN u.id END) as recurrent_customers,
        COUNT(DISTINCT CASE WHEN u.already_recurrent = false THEN u.id END) as new_customers
    FROM `order` o
    INNER JOIN user u ON o.user_id = u.id
    WHERE (:startDate IS NULL OR o.received_at >= :startDate)
      AND o.status IN :statuses
      AND o.received_at IS NOT NULL
    GROUP BY o.received_at
    ORDER BY date
    """, nativeQuery = true)
    List<Object[]> getCustomerTypeEvolution(
            @Param("startDate") LocalDate startDate,
            @Param("statuses") List<String> statuses);

    // Orders by Status
    @Query(value = """
    SELECT
        o.status,
        COUNT(o.id) as count
    FROM `order` o
    WHERE (:startDate IS NULL OR o.received_at >= :startDate)
    GROUP BY o.status
    ORDER BY count DESC
    """, nativeQuery = true)
    List<Object[]> getOrdersByStatus(@Param("startDate") LocalDate startDate);

    // Top Appliances - Most repaired
    @Query(value = """
    SELECT
        oi.type,
        oi.brand,
        COUNT(oi.id) as count
    FROM `order` o
    INNER JOIN order_item oi ON oi.order_id = o.id
    WHERE (:startDate IS NULL OR o.received_at >= :startDate)
      AND o.status IN :statuses
      AND oi.type IS NOT NULL
    GROUP BY oi.type, oi.brand
    ORDER BY count DESC
    LIMIT 10
    """, nativeQuery = true)
    List<Object[]> getTopAppliances(
            @Param("startDate") LocalDate startDate,
            @Param("statuses") List<String> statuses);

    // Top 10 Components/Parts Most Used
    @Query(value = """
    SELECT
        c.id,
        c.name,
        c.brand,
        c.category,
        SUM(d.quantity) as total_quantity
    FROM demand d
    INNER JOIN component c ON d.component_id = c.id
    INNER JOIN `order` o ON d.order_id = o.id
    WHERE (:startDate IS NULL OR o.received_at >= :startDate)
      AND o.status IN :statuses
    GROUP BY c.id, c.name, c.brand, c.category
    ORDER BY total_quantity DESC
    LIMIT 10
    """, nativeQuery = true)
    List<Object[]> getTopComponents(
            @Param("startDate") LocalDate startDate,
            @Param("statuses") List<String> statuses);

}
