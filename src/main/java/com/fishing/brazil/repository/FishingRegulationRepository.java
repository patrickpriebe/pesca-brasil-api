package com.fishing.brazil.repository;

import com.fishing.brazil.entity.FishingRegulation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FishingRegulationRepository extends JpaRepository<FishingRegulation, Long> {
    Page<FishingRegulation> findByHydrographicBasinContainingIgnoreCase(String basin, Pageable pageable);
}