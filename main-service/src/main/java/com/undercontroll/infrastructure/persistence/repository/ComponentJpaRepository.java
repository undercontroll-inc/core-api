package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.domain.entity.ComponentPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComponentJpaRepository extends JpaRepository<ComponentPart, Integer> {

    @Query("SELECT c FROM ComponentPart c WHERE c.name = :name")
    List<ComponentPart> findByName(@Param("name") String name);

    @Query("SELECT c FROM ComponentPart c WHERE c.category = :category")
    List<ComponentPart> findByCategory(@Param("category") String category);
}
