package com.codmer.turepulseai.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "business_leader_profiles")
public class BusinessLeaderProfile {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "entity_id")
    private EntityProfile entity;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String projectName;

    @Column(columnDefinition = "TEXT")
    private String projectDescription;

    @Column(nullable = false)
    private String mobileNumber;

    @Column(nullable = false)
    private String countryCode;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String contactHours;
}
