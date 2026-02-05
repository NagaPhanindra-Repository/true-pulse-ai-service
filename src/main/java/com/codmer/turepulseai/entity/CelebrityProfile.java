package com.codmer.turepulseai.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "celebrity_profiles")
public class CelebrityProfile {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "entity_id")
    private EntityProfile entity;

    @Column(nullable = false)
    private String realName;

    @Column(nullable = false)
    private String artistName;

    @Column(nullable = false)
    private String artistType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String mobileNumber;

    @Column(nullable = false)
    private String countryCode;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String contactHours;
}
