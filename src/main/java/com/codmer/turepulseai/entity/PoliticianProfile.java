package com.codmer.turepulseai.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "politician_profiles")
public class PoliticianProfile {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "entity_id")
    private EntityProfile entity;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String partyName;

    @Column(nullable = false)
    private String segmentAddress;

    @Column(nullable = false)
    private String contestingTo;

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
