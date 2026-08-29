package com.fishing.brazil.repository;

import com.fishing.brazil.entity.FishingSpot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FishingSpotRepository extends JpaRepository<FishingSpot, Long> {

    Page<FishingSpot> findByRiverId(Long riverId, Pageable pageable);

    /**
     * Pontos do mesmo rio dentro de uma caixa em torno da coordenada.
     *
     * <p>Uma caixa, e nao um raio: a distancia real exigiria trigonometria no banco
     * para separar dois pontos que estao a dezenas de metros um do outro, e nessa
     * escala a diferenca entre a caixa e o circulo nao muda nenhuma decisao.
     * Ordenado por id para que a escolha seja estavel entre chamadas.
     */
    @Query("SELECT s FROM FishingSpot s WHERE s.river.id = :riverId "
            + "AND s.latitude BETWEEN :minLatitude AND :maxLatitude "
            + "AND s.longitude BETWEEN :minLongitude AND :maxLongitude "
            + "ORDER BY s.id ASC")
    List<FishingSpot> findNearby(@Param("riverId") Long riverId,
                                 @Param("minLatitude") Double minLatitude,
                                 @Param("maxLatitude") Double maxLatitude,
                                 @Param("minLongitude") Double minLongitude,
                                 @Param("maxLongitude") Double maxLongitude);
}
