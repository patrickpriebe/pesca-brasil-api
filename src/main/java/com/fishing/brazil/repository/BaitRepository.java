package com.fishing.brazil.repository;

import com.fishing.brazil.entity.Bait;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BaitRepository extends JpaRepository<Bait, Long> {
    Page<Bait> findByNameContainingIgnoreCase(String name, Pageable pageable);
}