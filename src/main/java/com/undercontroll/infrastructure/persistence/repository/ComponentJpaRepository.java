package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.infrastructure.persistence.entity.ComponentPartJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComponentJpaRepository extends JpaRepository<ComponentPartJpaEntity, Integer> {

    @Query("SELECT c FROM ComponentPartJpaEntity c")
    List<ComponentPartJpaEntity> findSlice(Pageable pageable);

    @Query("SELECT c FROM ComponentPartJpaEntity c WHERE c.name = :name")
    List<ComponentPartJpaEntity> findByName(@Param("name") String name, Pageable pageable);

    @Query("""
            SELECT c FROM ComponentPartJpaEntity c
            WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))
            ORDER BY c.quantity ASC NULLS LAST
            """)
    List<ComponentPartJpaEntity> searchByName(@Param("name") String name, Pageable pageable);

    @Query("SELECT c FROM ComponentPartJpaEntity c WHERE c.category = :category")
    List<ComponentPartJpaEntity> findByCategory(@Param("category") String category, Pageable pageable);

    @Query("""
            SELECT c FROM ComponentPartJpaEntity c
            WHERE LOWER(c.category) LIKE LOWER(CONCAT('%', :category, '%'))
            ORDER BY c.quantity ASC NULLS LAST
            """)
    List<ComponentPartJpaEntity> searchByCategory(@Param("category") String category, Pageable pageable);

    @Query("SELECT c FROM ComponentPartJpaEntity c WHERE c.quantity IS NOT NULL AND c.quantity <= :maxQuantity ORDER BY c.quantity ASC")
    List<ComponentPartJpaEntity> findLowStock(@Param("maxQuantity") long maxQuantity, Pageable pageable);

    @Query("SELECT c FROM ComponentPartJpaEntity c ORDER BY c.quantity ASC NULLS LAST")
    List<ComponentPartJpaEntity> findLowestStock(Pageable pageable);
}
