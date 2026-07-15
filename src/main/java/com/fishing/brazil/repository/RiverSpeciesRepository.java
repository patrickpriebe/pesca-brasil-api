package com.fishing.brazil.repository;

import com.fishing.brazil.entity.RiverSpecies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RiverSpeciesRepository extends JpaRepository<RiverSpecies, Long> {
    Page<RiverSpecies> findByRiverId(Long riverId, Pageable pageable);
}