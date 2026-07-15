package com.fishing.brazil.entity;

import com.fishing.brazil.enums.EquipmentType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_equipment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    private EquipmentType type;

    private String recommendedLineWeight;
    private String action;
}