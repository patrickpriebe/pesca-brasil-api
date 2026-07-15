package com.fishing.brazil.repository;

import com.fishing.brazil.entity.Fish;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FishRepository extends JpaRepository<Fish, Long> {

    Page<Fish> findByCommonNameContainingIgnoreCase(String name, Pageable pageable);
}