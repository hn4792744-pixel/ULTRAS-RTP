package com.ultras.rtp.models;

/** How the Y coordinate is chosen. */
public enum HeightMode {
    /** Nether = SCAN, everything else = HIGHEST. */
    AUTO,
    /** Highest solid block (heightmap, ignoring leaves). */
    HIGHEST,
    /** Scan downward between scan-max-y and scan-min-y for the first safe spot. */
    SCAN
}
