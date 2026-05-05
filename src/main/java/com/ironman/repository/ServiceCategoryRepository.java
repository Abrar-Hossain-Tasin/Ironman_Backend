package com.ironman.repository;

import com.ironman.model.ServiceCategory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, UUID> {
  List<ServiceCategory> findByActiveTrueOrderByDisplayOrderAscNameAsc();
}
