package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.infrastructure.persistence.entity.ComponentPartJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComponentJpaRepository extends JpaRepository<ComponentPartJpaEntity, Integer> {

    @Query("SELECT c FROM ComponentPartJpaEntity c WHERE c.name = :name")
    List<ComponentPartJpaEntity> findByName(@Param("name") String name);

    @Query("SELECT c FROM ComponentPartJpaEntity c WHERE c.category = :category")
    List<ComponentPartJpaEntity> findByCategory(@Param("category") String category);
}
