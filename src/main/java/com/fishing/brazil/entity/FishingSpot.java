package com.fishing.brazil.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_fishing_spot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FishingSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "river_id", nullable = false)
    private River river;

    private String name;

    private Double latitude;
    private Double longitude;

    private String accessType;

    @Column(columnDefinition = "TEXT")
    private String description;
}