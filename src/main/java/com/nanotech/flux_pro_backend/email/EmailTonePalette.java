package com.nanotech.flux_pro_backend.email;

/**
 * Palette visuelle d'un gabarit email (styles inline).
 */
public record EmailTonePalette(String headerBg, String accentBar, String buttonBg) {

    public static EmailTonePalette forTone(String tone) {
        if (tone == null) {
            return navy();
        }
        return switch (tone.trim().toLowerCase()) {
            case "teal" -> new EmailTonePalette(
                    "linear-gradient(135deg,#0f4c5c 0%,#1a7a8c 100%)", "#5eead4", "#0f766e");
            case "amber" -> new EmailTonePalette(
                    "linear-gradient(135deg,#7a4a12 0%,#c47a1a 100%)", "#fbbf24", "#b45309");
            case "crimson" -> new EmailTonePalette(
                    "linear-gradient(135deg,#6b1d2a 0%,#a83245 100%)", "#fda4af", "#9f1239");
            case "forest" -> new EmailTonePalette(
                    "linear-gradient(135deg,#1b4332 0%,#2d6a4f 100%)", "#86efac", "#166534");
            case "slate" -> new EmailTonePalette(
                    "linear-gradient(135deg,#334155 0%,#475569 100%)", "#cbd5e1", "#475569");
            default -> navy();
        };
    }

    private static EmailTonePalette navy() {
        return new EmailTonePalette(
                "linear-gradient(135deg,#0b1f33 0%,#1e3a5f 100%)", "#93c5fd", "#1e3a5f");
    }
}
