package com.fishing.brazil.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "tb_fish")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Fish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String commonName;
    private String scientificName;
    private String conservationStatus;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String imageUrl;

    @ManyToMany
    @JoinTable(
            name = "tb_fish_bait",
            joinColumns = @JoinColumn(name = "fish_id"),
            inverseJoinColumns = @JoinColumn(name = "bait_id")
    )
    private List<Bait> recommendedBaits;

    @ManyToMany
    @JoinTable(
            name = "tb_fish_equipment",
            joinColumns = @JoinColumn(name = "fish_id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_id")
    )
    private List<Equipment> recommendedEquipments;
}