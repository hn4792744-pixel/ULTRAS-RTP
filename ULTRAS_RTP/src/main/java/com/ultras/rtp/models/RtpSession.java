package com.ultras.rtp.models;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/** One active RTP (countdown + search + teleport) for one player. */
public final class RtpSession {
    public final UUID id;
    public final String targetWorld;
    public final Location start;
    public final boolean checkMove;
    public final double threshold;
    public int secondsLeft;
    public int ticks;
    /** True once the countdown is over (movement/death no longer cancel). */
    public volatile boolean searching;
    private BukkitTask task;

    public RtpSession(UUID id, String targetWorld, Location start, int seconds, boolean checkMove, double threshold) {
        this.id = id;
        this.targetWorld = targetWorld;
        this.start = start;
        this.secondsLeft = seconds;
        this.checkMove = checkMove;
        this.threshold = Math.max(0.05D, threshold);
    }

    public void setTask(BukkitTask task) {
        this.task = task;
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
