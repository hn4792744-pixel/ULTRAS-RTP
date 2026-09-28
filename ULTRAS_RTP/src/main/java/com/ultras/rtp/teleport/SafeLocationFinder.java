package com.ultras.rtp.teleport;

import com.ultras.rtp.UltrasRTP;
import com.ultras.rtp.models.HeightMode;
import com.ultras.rtp.models.WorldSettings;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Finds a random safe location. Chunks are loaded with Paper's async chunk API, every Bukkit block
 * access happens on the main thread, and attempts are chained across ticks (never blocks the server).
 */
public final class SafeLocationFinder {
    private static final int BORDER_MARGIN = 8;

    private static final Set<Material> HAZARDS = EnumSet.of(
            Material.LAVA, Material.LAVA_CAULDRON, Material.FIRE, Material.SOUL_FIRE, Material.CACTUS,
            Material.MAGMA_BLOCK, Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.POWDER_SNOW,
            Material.SWEET_BERRY_BUSH, Material.WITHER_ROSE, Material.POINTED_DRIPSTONE, Material.COBWEB,
            Material.TNT, Material.NETHER_PORTAL, Material.END_PORTAL, Material.END_GATEWAY,
            Material.BUBBLE_COLUMN);

    private final UltrasRTP plugin;

    public SafeLocationFinder(UltrasRTP plugin) {
        this.plugin = plugin;
    }

    /** Must be called on the main thread. The future completes with null when nothing safe was found. */
    public CompletableFuture<Location> find(World world, WorldSettings s) {
        CompletableFuture<Location> result = new CompletableFuture<>();
        double half = world.getWorldBorder().getSize() / 2.0D - BORDER_MARGIN;
        int effectiveMax = (int) Math.min(s.maxRadius(), Math.floor(half));
        if (effectiveMax <= 0 || effectiveMax <= s.minRadius()) {
            result.complete(null);
            return result;
        }
        attempt(world, s, effectiveMax, 0, result);
        return result;
    }

    private void attempt(World world, WorldSettings s, int max, int n, CompletableFuture<Location> result) {
        if (n >= s.maxAttempts()) {
            result.complete(null);
            return;
        }
        int[] xz = randomPoint(s, max);
        if (!insideBorder(world, xz[0], xz[1])) {
            attempt(world, s, max, n + 1, result);
            return;
        }
        world.getChunkAtAsync(xz[0] >> 4, xz[1] >> 4, true).whenComplete((chunk, error) -> sync(result, () -> {
            if (error != null || chunk == null) {
                attempt(world, s, max, n + 1, result);
                return;
            }
            Location found = null;
            try {
                found = evaluate(world, s, xz[0], xz[1]);
            } catch (RuntimeException ex) {
                plugin.getLogger().log(Level.WARNING, "RTP location check failed", ex);
            }
            if (found != null) {
                result.complete(found);
            } else {
                attempt(world, s, max, n + 1, result);
            }
        }));
    }

    private void sync(CompletableFuture<Location> result, Runnable task) {
        if (!plugin.isEnabled()) {
            result.complete(null);
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /** Uniform distribution over the annulus [min, max] (area-weighted radius, so no bias to the centre). */
    private static int[] randomPoint(WorldSettings s, int max) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double min = s.minRadius();
        double radius = Math.sqrt(r.nextDouble() * (max * (double) max - min * min) + min * min);
        double angle = r.nextDouble() * Math.PI * 2.0D;
        return new int[]{
                (int) Math.floor(s.centerX() + Math.cos(angle) * radius),
                (int) Math.floor(s.centerZ() + Math.sin(angle) * radius)};
    }

    private static boolean insideBorder(World world, int x, int z) {
        WorldBorder border = world.getWorldBorder();
        Location c = border.getCenter();
        double half = border.getSize() / 2.0D - BORDER_MARGIN;
        return Math.abs(x + 0.5D - c.getX()) <= half && Math.abs(z + 0.5D - c.getZ()) <= half;
    }

    private Location evaluate(World world, WorldSettings s, int x, int z) {
        HeightMode mode = s.heightMode();
        boolean nether = world.getEnvironment() == World.Environment.NETHER;
        if (mode == HeightMode.AUTO) {
            mode = nether ? HeightMode.SCAN : HeightMode.HIGHEST;
        }
        if (mode == HeightMode.HIGHEST) {
            int y = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            return check(world, s, x, y, z);
        }
        int minY = s.scanMinY() != null ? s.scanMinY() : (nether ? 32 : world.getMinHeight() + 2);
        int maxY = s.scanMaxY() != null ? s.scanMaxY() : (nether ? 120 : world.getMaxHeight() - 3);
        minY = Math.max(minY, world.getMinHeight() + 1);
        maxY = Math.min(maxY, world.getMaxHeight() - 3);
        for (int y = maxY; y >= minY; y--) {
            Location loc = check(world, s, x, y, z);
            if (loc != null) {
                return loc;
            }
        }
        return null;
    }

    /** floorY = the block the player stands on. */
    private Location check(World world, WorldSettings s, int x, int floorY, int z) {
        if (floorY <= world.getMinHeight() || floorY + 3 >= world.getMaxHeight()) {
            return null;
        }
        Block floor = world.getBlockAt(x, floorY, z);
        Block feet = floor.getRelative(BlockFace.UP);
        Block head = feet.getRelative(BlockFace.UP);
        Block above = head.getRelative(BlockFace.UP);
        Material floorType = floor.getType();

        if (floorType == Material.WATER) {
            if (!s.allowWater()) {
                return null;
            }
        } else if (!floorType.isSolid() || floor.isPassable() || HAZARDS.contains(floorType) || Tag.LEAVES.isTagged(floorType)) {
            return null;
        }
        if (!isFree(feet) || !isFree(head) || above.getType() == Material.LAVA) {
            return null;
        }
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
            if (HAZARDS.contains(feet.getRelative(face).getType())) {
                return null;
            }
        }
        if (!s.blockedBiomes().isEmpty() && s.blockedBiomes().contains(feet.getBiome().getKey().getKey())) {
            return null;
        }
        return new Location(world, x + 0.5D, floorY + 1.0D, z + 0.5D);
    }

    private static boolean isFree(Block block) {
        return block.isPassable() && !block.isLiquid() && !HAZARDS.contains(block.getType());
    }
}
