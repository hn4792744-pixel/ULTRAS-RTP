package com.ultras.rtp.teleport;

import com.ultras.rtp.UltrasRTP;
import com.ultras.rtp.configuration.ConfigManager;
import com.ultras.rtp.language.LanguageManager;
import com.ultras.rtp.models.RtpSession;
import com.ultras.rtp.models.WorldSettings;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** Core RTP flow: permission -> cooldown -> world resolution -> countdown -> search -> teleport. */
public final class RtpManager {
    private record Resolution(WorldSettings target, String errorKey) {
    }

    private final UltrasRTP plugin;
    private final SafeLocationFinder finder;
    private final Map<UUID, RtpSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public RtpManager(UltrasRTP plugin) {
        this.plugin = plugin;
        this.finder = new SafeLocationFinder(plugin);
    }

    // ───────────────────────── entry points ─────────────────────────

    /** /rtp */
    public void handleCommand(Player p) {
        ConfigManager cfg = plugin.config();
        if (!has(p, cfg.permUse())) {
            deny(p, "no-permission");
            return;
        }
        if (sessions.containsKey(p.getUniqueId())) {
            deny(p, "already-in-progress");
            return;
        }
        if (cfg.guiEnabled()) {
            plugin.gui().open(p);
            return;
        }
        Resolution r = resolve(p);
        if (r.target() == null) {
            deny(p, r.errorKey());
            return;
        }
        begin(p, r.target());
    }

    /** Priority: current world enabled -> its own fallback -> global fallback -> not available. */
    private Resolution resolve(Player p) {
        ConfigManager cfg = plugin.config();
        WorldSettings current = plugin.worlds().get(p.getWorld().getName());
        if (current != null && current.enabled()) {
            return new Resolution(current, null);
        }
        boolean fallback = current != null && current.fallbackToWorld() != null
                ? current.fallbackToWorld()
                : cfg.fallbackEnabled();
        if (!fallback) {
            return new Resolution(null, "not-available");
        }
        WorldSettings target = plugin.worlds().get(cfg.fallbackTarget());
        if (!plugin.worlds().isUsable(target)) {
            return new Resolution(null, "world-unavailable");
        }
        return new Resolution(target, null);
    }

    /** Starts the RTP flow for an already chosen target world (also used by the GUI). */
    public void begin(Player p, WorldSettings target) {
        ConfigManager cfg = plugin.config();
        UUID id = p.getUniqueId();
        if (!has(p, cfg.permUse())) {
            deny(p, "no-permission");
            return;
        }
        if (!target.permission().isBlank() && !has(p, target.permission())) {
            deny(p, "world-no-permission");
            return;
        }
        if (sessions.containsKey(id)) {
            deny(p, "already-in-progress");
            return;
        }
        if (!plugin.worlds().isUsable(target)) {
            deny(p, "world-unavailable");
            return;
        }
        boolean bypassCooldown = has(p, cfg.permBypassCooldown());
        if (!bypassCooldown) {
            long remaining = cooldownSeconds(id);
            if (remaining > 0) {
                deny(p, "cooldown", LanguageManager.time(remaining));
                return;
            }
        }
        boolean checkMove = target.cancelOnMove() && !has(p, cfg.permBypassMove());
        RtpSession session = new RtpSession(id, target.name(), p.getLocation().clone(),
                target.countdown(), checkMove, target.movementThreshold());
        if (sessions.putIfAbsent(id, session) != null) {
            deny(p, "already-in-progress");
            return;
        }
        if (cfg.cooldownOnBegin() && !bypassCooldown && target.cooldown() > 0) {
            cooldowns.put(id, System.currentTimeMillis() + target.cooldown() * 1000L);
        }
        if (session.secondsLeft <= 0) {
            search(p, session);
            return;
        }
        plugin.lang().send(p, "countdown-begin", LanguageManager.time(session.secondsLeft));
        plugin.sounds().play(p, "countdown-start");
        actionbar(p, session.secondsLeft);
        session.setTask(Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(session), 4L, 4L));
    }

    // ───────────────────────── countdown ─────────────────────────

    private void tick(RtpSession s) {
        Player p = Bukkit.getPlayer(s.id);
        if (p == null || !p.isOnline() || sessions.get(s.id) != s) {
            end(s);
            return;
        }
        if (p.isDead()) {
            cancel(p, s, "cancelled-death");
            return;
        }
        if (!p.getWorld().getUID().equals(s.start.getWorld().getUID())) {
            cancel(p, s, "cancelled-world-change");
            return;
        }
        if (s.checkMove && p.getLocation().distanceSquared(s.start) > s.threshold * s.threshold) {
            cancel(p, s, "cancelled-moved");
            return;
        }
        s.ticks += 4;
        if (s.ticks % 20 != 0) {
            return;
        }
        s.secondsLeft--;
        if (s.secondsLeft <= 0) {
            s.stop();
            search(p, s);
            return;
        }
        actionbar(p, s.secondsLeft);
        plugin.sounds().play(p, "countdown");
    }

    private void actionbar(Player p, int seconds) {
        if (plugin.config().actionbarEnabled()) {
            p.sendActionBar(plugin.lang().component("countdown-actionbar", LanguageManager.time(seconds)));
        }
    }

    // ───────────────────────── search + teleport ─────────────────────────

    private void search(Player p, RtpSession s) {
        s.searching = true;
        WorldSettings target = plugin.worlds().get(s.targetWorld); // fresh settings (reload-safe)
        World world = Bukkit.getWorld(s.targetWorld);
        if (target == null || !target.enabled() || world == null) {
            fail(p, s, "world-unavailable");
            return;
        }
        finder.find(world, target).whenComplete((loc, error) -> runSync(() -> onFound(s, target, loc, error)));
    }

    private void onFound(RtpSession s, WorldSettings target, Location loc, Throwable error) {
        Player p = Bukkit.getPlayer(s.id);
        if (p == null || !p.isOnline() || sessions.get(s.id) != s || p.isDead()) {
            end(s);
            return;
        }
        if (error != null) {
            plugin.getLogger().log(Level.WARNING, "RTP search failed", error);
        }
        if (error != null || loc == null) {
            fail(p, s, "no-safe-location");
            return;
        }
        Location dest = loc.clone();
        dest.setYaw(p.getLocation().getYaw());
        dest.setPitch(p.getLocation().getPitch());
        p.teleportAsync(dest, PlayerTeleportEvent.TeleportCause.PLUGIN).whenComplete((ok, err) -> runSync(() -> {
            Player pl = Bukkit.getPlayer(s.id);
            end(s);
            if (pl == null) {
                return;
            }
            if (err != null || !Boolean.TRUE.equals(ok)) {
                deny(pl, "teleport-failed");
                return;
            }
            if (!plugin.config().cooldownOnBegin() && !has(pl, plugin.config().permBypassCooldown()) && target.cooldown() > 0) {
                cooldowns.put(s.id, System.currentTimeMillis() + target.cooldown() * 1000L);
            }
            plugin.lang().send(pl, "success");
            plugin.sounds().play(pl, "success");
        }));
    }

    // ───────────────────────── session control ─────────────────────────

    private void end(RtpSession s) {
        s.stop();
        sessions.remove(s.id, s);
    }

    private void cancel(Player p, RtpSession s, String key) {
        end(s);
        plugin.lang().send(p, key);
        plugin.sounds().play(p, "cancelled");
    }

    private void fail(Player p, RtpSession s, String key) {
        end(s);
        deny(p, key);
    }

    /** Cancels only while the countdown is running (never during search/teleport). */
    public void cancelCountdown(UUID id, String key) {
        RtpSession s = sessions.get(id);
        if (s == null || s.searching) {
            return;
        }
        Player p = Bukkit.getPlayer(id);
        if (p == null) {
            end(s);
            return;
        }
        cancel(p, s, key);
    }

    public void quit(UUID id) {
        RtpSession s = sessions.get(id);
        if (s != null) {
            end(s);
        }
    }

    public void shutdown() {
        for (RtpSession s : sessions.values()) {
            s.stop();
        }
        sessions.clear();
    }

    // ───────────────────────── helpers ─────────────────────────

    private long cooldownSeconds(UUID id) {
        Long expiry = cooldowns.get(id);
        if (expiry == null) {
            return 0;
        }
        long ms = expiry - System.currentTimeMillis();
        if (ms <= 0) {
            cooldowns.remove(id);
            return 0;
        }
        return (ms + 999L) / 1000L;
    }

    private boolean has(Player p, String node) {
        return node == null || node.isBlank() || p.hasPermission(node) || p.hasPermission(plugin.config().permAdmin());
    }

    private void deny(Player p, String key, TagResolver... resolvers) {
        plugin.lang().send(p, key, resolvers);
        plugin.sounds().play(p, soundFor(key));
    }

    private static String soundFor(String key) {
        return switch (key) {
            case "cooldown" -> "cooldown";
            case "no-safe-location" -> "no-safe-location";
            case "not-available", "world-no-permission", "world-unavailable", "gui-world-disabled" -> "world-not-allowed";
            default -> "error";
        };
    }

    private void runSync(Runnable task) {
        if (!plugin.isEnabled()) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}
