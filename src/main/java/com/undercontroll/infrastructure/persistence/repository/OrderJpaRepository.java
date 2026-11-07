package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Integer> {

    List<OrderJpaEntity> findByUser_id(Integer userId);

    @EntityGraph(attributePaths = {"user", "orderItems"})
    @Query("SELECT o FROM OrderJpaEntity o")
    List<OrderJpaEntity> findRecentWithUserAndItems(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "orderItems"})
    List<OrderJpaEntity> findByStatus(OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "orderItems"})
    List<OrderJpaEntity> findByStatusIn(Collection<OrderStatus> statuses, Pageable pageable);

    long countByStatusIn(Collection<OrderStatus> statuses);

    @EntityGraph(attributePaths = {"user"})
    Page<OrderJpaEntity> findPageByStatusIn(Collection<OrderStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "orderItems"})
    @Query("SELECT o FROM OrderJpaEntity o WHERE o.id = :id")
    Optional<OrderJpaEntity> findDetailById(@Param("id") Integer id);

    @Query("SELECT o FROM OrderJpaEntity o")
    Page<OrderJpaEntity> findAllPaginated(Pageable pageable);

    @Query("SELECT o FROM OrderJpaEntity o JOIN o.orderItems oi WHERE oi.id = :orderItemId")
    Optional<OrderJpaEntity> findOrderByOrderItemId(@Param("orderItemId") Integer orderItemId);

    @Query(value = """
    SELECT COALESCE(SUM(o.total), 0.0)
    FROM orders o
    WHERE (CAST(:startDate AS DATE) IS NULL OR o.received_at >= CAST(:startDate AS DATE))
      AND o.status IN :statuses
    """, nativeQuery = true)
    Double calculateTotalRevenueFiltered(
            @Param("startDate") LocalDate startDate,
            @Param("statuses") List<String> statuses);

    @Query(value = """
    SELECT COALESCE(SUM(c.price * d.quantity), 0.0)
    FROM orders o
    INNER JOIN demand d ON o.id = d.order_id
    INNER JOIN component c ON d.component_id = c.id
    WHERE (CAST(:startDate AS DATE) IS NULL OR o.received_at >= CAST(:startDate AS DATE))
      AND o.status IN :statuses
    """, nativeQuery = true)
    Double calculateTotalPartsCostFiltered(
            @Param("startDate") LocalDate startDate,
            @Param("statuses") List<String> statuses);

    @Query(value = """
    SELECT COALESCE(AVG(o.total), 0.0)
    FROM orders o
    WHERE (CAST(:startDate AS DATE) IS NULL OR o.received_at >= CAST(:startDate AS DATE))
      AND o.status IN :statuses
    """, nativeQuery = true)
    Double calculateAverageOrderPriceFiltered(
            @Param("startDate") LocalDate startDate,
            @Param("statuses") List<String> statuses);

    @Query(value = """
    SELECT COUNT(o.id)
    FROM orders o
    WHERE (CAST(:startDate AS DATE) IS NULL OR o.received_at >= CAST(:startDate AS DATE))
      AND o.status IN :statuses
    """, nativeQuery = true)
    Long countOngoingOrdersFiltered(
            @Param("startDate") LocalDate startDate,
            @Param("statuses") List<String> statuses);

    @Query(value = """
    SELECT COALESCE(AVG((completed_time - received_at) * 24), 0.0)
    FROM orders
    WHERE (CAST(:startDate AS DATE) IS NULL OR received_at >= CAST(:startDate AS DATE))
      AND status IN :statuses
      AND received_at IS NOT NULL
      AND completed_time IS NOT NULL
    """, nativeQuery = true)
    Double calculateAverageRepairTimeFiltered(
            @Param("startDate") LocalDate startDate,
            @Param("statuses") List<String> statuses);

    @Query(value = """
    SELECT
        o.received_at AS date,
        COALESCE(SUM(o.total), 0.0) AS revenue,
        COALESCE(SUM(o.total) - SUM(COALESCE(parts_cost, 0)), 0.0) AS profit,
        COUNT(o.id) AS order_count
    FROM orders o
    LEFT JOIN (
        SELECT d.order_id, SUM(c.price * d.quantity) AS parts_cost
        FROM demand d
        INNER JOIN component c ON d.component_id = c.id
        GROUP BY d.order_id
    ) parts ON o.id = parts.order_id
    WHERE (CAST(:startDate AS DATE) IS NULL OR o.received_at >= CAST(:startDate AS DATE))
      AND o.status IN :statuses
      AND o.received_at IS NOT NULL
    GROUP BY o.received_at
    ORDER BY o.received_at
    """, nativeQuery = true)
    List<Object[]> getRevenueEvolution(
            @Param("startDate") LocalDate startDate,
            @Param("statuses") List<String> statuses);

    @Query(value = """
    SELECT
        o.received_at AS date,
        COUNT(DISTINCT CASE WHEN u.already_recurrent = true THEN u.id END) AS recurrent_customers,
        COUNT(DISTINCT CASE WHEN u.already_recurrent = false THEN u.id END) AS new_customers
    FROM orders o
    INNER JOIN "user" u ON o.user_id = u.id
    WHERE (CAST(:startDate AS DATE) IS NULL OR o.received_at >= CAST(:startDate AS DATE))
      AND o.status IN :statuses
      AND o.received_at IS NOT NULL
    GROUP BY o.received_at
    ORDER BY o.received_at
    """, nativeQuery = true)
    List<Object[]> getCustomerTypeEvolution(
            @Param("startDate") LocalDate startDate,
            @Param("statuses") List<String> statuses);

    @Query(value = """
    SELECT
        o.status,
        COUNT(o.id) AS status_count
    FROM orders o
    WHERE (CAST(:startDate AS DATE) IS NULL OR o.received_at >= CAST(:startDate AS DATE))
    GROUP BY o.status
    ORDER BY 2 DESC
    """, nativeQuery = true)
    List<Object[]> getOrdersByStatus(@Param("startDate") LocalDate startDate);

    @Query(value = """
    SELECT
        oi.type,
        oi.brand,
        COUNT(oi.id) AS appliance_count
    FROM orders o
    INNER JOIN order_item oi ON oi.order_id = o.id
    WHERE (CAST(:startDate AS DATE) IS NULL OR o.received_at >= CAST(:startDate AS DATE))
      AND o.status IN :statuses
      AND oi.type IS NOT NULL
    GROUP BY oi.type, oi.brand
    ORDER BY 3 DESC
    LIMIT 10
    """, nativeQuery = true)
    List<Object[]> getTopAppliances(
            @Param("startDate") LocalDate startDate,
            @Param("statuses") List<String> statuses);

    @Query(value = """
    SELECT
        c.id,
        c.name,
        c.brand,
        c.category,
        SUM(d.quantity) AS total_quantity
    FROM demand d
    INNER JOIN component c ON d.component_id = c.id
    INNER JOIN orders o ON d.order_id = o.id
    WHERE (CAST(:startDate AS DATE) IS NULL OR o.received_at >= CAST(:startDate AS DATE))
      AND o.status IN :statuses
    GROUP BY c.id, c.name, c.brand, c.category
    ORDER BY total_quantity DESC
    LIMIT 10
    """, nativeQuery = true)
    List<Object[]> getTopComponents(
            @Param("startDate") LocalDate startDate,
            @Param("statuses") List<String> statuses);

    @Query(value = """
    SELECT
        oi.brand,
        oi.model,
        oi.type,
        COUNT(oi.id) AS volume
    FROM orders o
    INNER JOIN order_item oi ON oi.order_id = o.id
    WHERE (CAST(:startDate AS DATE) IS NULL OR o.received_at >= CAST(:startDate AS DATE))
      AND oi.brand IS NOT NULL
    GROUP BY oi.brand, oi.model, oi.type
    ORDER BY volume DESC
    """, nativeQuery = true)
    List<Object[]> getRepairCatalog(@Param("startDate") LocalDate startDate);

}
