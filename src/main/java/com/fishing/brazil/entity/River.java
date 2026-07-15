package com.fishing.brazil.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_river")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class River {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String name;

    @Column(name = "hydrographic_basin")
    private String hydrographicBasin;

    @Column(columnDefinition = "TEXT")
    private String description;
}