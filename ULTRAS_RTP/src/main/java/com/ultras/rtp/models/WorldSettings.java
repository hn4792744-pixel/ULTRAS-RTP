package com.ultras.rtp.models;

import java.util.Set;

/** Immutable per-world RTP settings (snapshot of config.yml). */
public record WorldSettings(
        String name,
        String displayName,
        boolean enabled,
        int minRadius,
        int maxRadius,
        int centerX,
        int centerZ,
        int cooldown,
        int countdown,
        boolean cancelOnMove,
        double movementThreshold,
        int maxAttempts,
        boolean allowWater,
        HeightMode heightMode,
        Integer scanMinY,
        Integer scanMaxY,
        Boolean fallbackToWorld,
        String permission,
        Set<String> blockedBiomes) {
}
