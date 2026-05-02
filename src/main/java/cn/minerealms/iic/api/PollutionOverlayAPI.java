package cn.minerealms.iic.api;

import net.minecraft.world.level.ChunkPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API for XaerosWorldMap integration - provides pollution data for overlay rendering.
 * <p>
 * This API is thread-safe and can be called from both client and server threads.
 * The pollution cache is automatically updated by {@link cn.minerealms.iic.pollution.PollutionManager}
 * during pollution processing.
 *
 * @author ImprovedMobs Industrial Integration
 */
public class PollutionOverlayAPI {

    /** Thread-safe pollution cache for client-side rendering */
    private static final Map<ChunkPos, Double> pollutionCache = new ConcurrentHashMap<>();

    /**
     * Gets the pollution level for a specific chunk.
     * <p>
     * This method is thread-safe and can be called from the render thread.
     *
     * @param chunkPos The chunk position to query
     * @return Pollution value (0.0 - 300.0+), or 0.0 if no pollution
     */
    public static double getPollutionForChunk(ChunkPos chunkPos) {
        return pollutionCache.getOrDefault(chunkPos, 0.0);
    }

    /**
     * Updates the pollution cache for a specific chunk.
     * <p>
     * This method is called by {@link cn.minerealms.iic.pollution.PollutionManager}
     * during pollution processing. Chunks with pollution <= 0.1 are automatically removed.
     *
     * @param chunkPos The chunk position to update
     * @param pollution The new pollution value
     */
    public static void updatePollutionCache(ChunkPos chunkPos, double pollution) {
        if (pollution > 0.1) {
            pollutionCache.put(chunkPos, pollution);
        } else {
            pollutionCache.remove(chunkPos);
        }
    }

    /**
     * Gets all chunks with pollution > 0.
     * <p>
     * Returns a snapshot of the current pollution state. The returned map
     * is a copy and can be safely iterated without synchronization.
     *
     * @return A copy of the pollution cache
     */
    public static Map<ChunkPos, Double> getAllPollutedChunks() {
        return new ConcurrentHashMap<>(pollutionCache);
    }

    /**
     * Clears the pollution cache.
     * <p>
     * This should be called when changing dimensions or disconnecting from a server
     * to prevent stale data from being displayed.
     */
    public static void clearCache() {
        pollutionCache.clear();
    }
}
