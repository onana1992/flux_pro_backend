package com.nanotech.flux_pro_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Singleton (une seule ligne) stockant l'ancre de l'horloge artificielle en mode test.
 * Temps effectif = artificialNow + (wallNow - wallSyncedAt).
 */
@Entity
@Table(name = "system_clock")
@Getter
@Setter
public class SystemClockState extends BaseEntity {

    @Column(name = "artificial_now", nullable = false)
    private Instant artificialNow;

    @Column(name = "wall_synced_at", nullable = false)
    private Instant wallSyncedAt;
}
