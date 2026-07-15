package com.fishing.brazil.entity;

import com.fishing.brazil.enums.BaitType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_bait")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Bait {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private BaitType type;

    @Column(columnDefinition = "TEXT")
    private String description;
}