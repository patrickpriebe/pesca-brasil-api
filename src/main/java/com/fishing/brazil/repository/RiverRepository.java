package com.fishing.brazil.repository;

import com.fishing.brazil.entity.River;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RiverRepository extends JpaRepository<River, Long> {
    Page<River> findByNameContainingIgnoreCase(String name, Pageable pageable);
}