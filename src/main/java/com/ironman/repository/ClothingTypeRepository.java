package com.ironman.repository;

import com.ironman.model.ClothingType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothingTypeRepository extends JpaRepository<ClothingType, UUID> {
  List<ClothingType> findByActiveTrueOrderByDisplayOrderAscNameAsc();
}
