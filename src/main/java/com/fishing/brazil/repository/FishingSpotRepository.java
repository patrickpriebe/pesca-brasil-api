package com.fishing.brazil.repository;

import com.fishing.brazil.entity.FishingSpot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FishingSpotRepository extends JpaRepository<FishingSpot, Long> {

    Page<FishingSpot> findByRiverId(Long riverId, Pageable pageable);
}