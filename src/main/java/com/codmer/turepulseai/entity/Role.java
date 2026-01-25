package com.codmer.turepulseai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;
import java.util.HashSet;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "roles", indexes = {@Index(name = "idx_role_name", columnList = "name")})
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();
}

