package com.fishing.brazil.repository;

import com.fishing.brazil.entity.CatchRecord;
import com.fishing.brazil.repository.projection.RankingPescadorProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatchRecordRepository extends JpaRepository<CatchRecord, Long> {

    Page<CatchRecord> findByFishingSpotId(Long spotId, Pageable pageable);
    Page<CatchRecord> findByFishId(Long fishId, Pageable pageable);

    Page<CatchRecord> findByUserEmail(String email, Pageable pageable);

    @Query("SELECT c FROM CatchRecord c WHERE "
            + "(:termo IS NULL OR LOWER(c.fish.commonName) LIKE LOWER(CONCAT('%', :termo, '%')) "
            + "OR LOWER(c.fishingSpot.name) LIKE LOWER(CONCAT('%', :termo, '%')))")
    Page<CatchRecord> buscarComFiltro(@Param("termo") String termo, Pageable pageable);

    List<CatchRecord> findTop10ByLengthInCmIsNotNullOrderByLengthInCmDesc();

    List<CatchRecord> findTop10ByWeightInKgIsNotNullOrderByWeightInKgDesc();

    /**
     * O campo se chama "dias na agua", entao ele conta datas distintas.
     *
     * <p>A versao anterior contava COUNT(DISTINCT c.catchDate) — instantes distintos —
     * e duas capturas com uma hora de intervalo na mesma pescaria valiam dois dias.
     * O CAST para date e o que faz o nome e a consulta dizerem a mesma coisa.
     */
    @Query("SELECT c.user.name AS pescador, COUNT(c.id) AS capturas, "
            + "COUNT(DISTINCT CAST(c.catchDate AS date)) AS diasNaAgua "
            + "FROM CatchRecord c GROUP BY c.user.name ORDER BY COUNT(c.id) DESC")
    List<RankingPescadorProjection> findTopPescadores(Pageable pageable);
}
