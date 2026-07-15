package com.fishing.brazil.entity;

import com.fishing.brazil.enums.CatchOutcome;
import com.fishing.brazil.enums.MoonPhase;
import com.fishing.brazil.enums.WeatherCondition;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class CatchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private com.fishing.brazil.entity.login.User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fish_id", nullable = false)
    private Fish fish;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fishing_spot_id", nullable = false)
    private FishingSpot fishingSpot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bait_id")
    private Bait bait;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id")
    private Equipment equipment;

    private Double weightInKg;
    private Double lengthInCm;

    @Column(nullable = false)
    private LocalDateTime catchDate;

    @Enumerated(EnumType.STRING)
    private WeatherCondition weatherCondition;

    @Enumerated(EnumType.STRING)
    private MoonPhase moonPhase;

    @Enumerated(EnumType.STRING)
    private CatchOutcome outcome;

    private String photoUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;
}