package com.fishing.brazil.entity;

import com.fishing.brazil.enums.Abundance;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_river_species")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RiverSpecies {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "river_id", nullable = false)
    private River river;

    @ManyToOne
    @JoinColumn(name = "fish_id", nullable = false)
    private Fish fish;

    @Enumerated(EnumType.STRING)
    private Abundance abundance;

    private String bestSeason;
}