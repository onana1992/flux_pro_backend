package com.nanotech.flux_pro_backend.service;

/**
 * Publié après un saut / repositionnement explicite de l'horloge (set, adjust, reset).
 * Le {@link ClockDrivenJobScheduler} rattrape alors les créneaux cron simulés franchis.
 */
public record SystemClockMovedEvent(java.time.Instant previousSimulatedNow, java.time.Instant newSimulatedNow) {
}
