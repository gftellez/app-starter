package com.example.app.tenant;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Whoever an application's data belongs to — a family, a company, a team. Rename it to what
 * it actually is in this app; the mechanism around it is what matters.
 */
@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;
}
