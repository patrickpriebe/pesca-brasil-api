package com.fishing.brazil.repository;

import com.fishing.brazil.entity.RiverSpecies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RiverSpeciesRepository extends JpaRepository<RiverSpecies, Long> {

    Page<RiverSpecies> findByRiverId(Long riverId, Pageable pageable);

    /**
     * O par rio + peixe descreve um fato unico ("dourado e ALTA no Parana"). Sem esta
     * checagem a mesma associacao podia ser inserida varias vezes, cada uma com uma
     * abundancia diferente, e nenhuma delas era mais verdadeira que a outra.
     *
     * <p>A restricao definitiva pertence ao banco, como UNIQUE(river_id, fish_id). Ela
     * depende da migracao versionada: com ddl-auto=update, criar a constraint numa
     * tabela que ja tem duplicatas falha, e a aplicacao subiria sem ela.
     */
    boolean existsByRiverIdAndFishId(Long riverId, Long fishId);
}
