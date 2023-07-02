package com.matchUpSports.boundedContext.field.repository;

import com.matchUpSports.boundedContext.field.entity.Field;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FieldRepository extends JpaRepository<Field, Long> {
    // FieldRepository.java
    public List<Field> findByFieldLocation(String location);

}
