package net.vulkanshaders.compiler;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Caches compiled SPIR-V bytecode to disk for faster loading
 */
public class ShaderCache {
    private static final Logger LOGGER = LoggerFactory.getLogger("VulkanShaders/Cache");
    private final Path cacheDir;
    private final Map<String, ByteBuffer> memoryCache;

    public ShaderCache() {
        this.cacheDir = FabricLoader.getInstance()
                .getGameDir()
                .resolve("shadercache");
        this.memoryCache = new HashMap<>();

        // Create cache directory
        try {
            Files.createDirectories(cacheDir);
            LOGGER.info("Shader cache directory: {}", cacheDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create shader cache directory", e);
        }
    }

    /**
     * Get cached SPIR-V bytecode
     *
     * @param key Cache key (SHA-256 hash)
     * @return Cached bytecode, or null if not found
     */
    public ByteBuffer get(String key) {
        // Check memory cache first
        if (memoryCache.containsKey(key)) {
            return memoryCache.get(key);
        }

        // Check disk cache
        Path cachePath = cacheDir.resolve(key + ".spv");
        if (!Files.exists(cachePath)) {
            return null;
        }

        try {
            byte[] bytes = Files.readAllBytes(cachePath);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);

            // Store in memory cache
            memoryCache.put(key, buffer);

            return buffer;
        } catch (IOException e) {
            LOGGER.warn("Failed to read cached shader: {}", key, e);
            return null;
        }
    }

    /**
     * Store SPIR-V bytecode in cache
     *
     * @param key Cache key
     * @param bytecode Compiled SPIR-V bytecode
     */
    public void put(String key, ByteBuffer bytecode) {
        // Store in memory
        memoryCache.put(key, bytecode);

        // Store on disk
        Path cachePath = cacheDir.resolve(key + ".spv");

        try {
            // Convert ByteBuffer to byte array
            byte[] bytes = new byte[bytecode.remaining()];
            bytecode.duplicate().get(bytes); // Use duplicate to avoid affecting position

            Files.write(cachePath, bytes);
            LOGGER.debug("Cached shader: {}", key);
        } catch (IOException e) {
            LOGGER.warn("Failed to cache shader: {}", key, e);
        }
    }

    /**
     * Clear all cached shaders
     */
    public void clear() {
        memoryCache.clear();

        try {
            Files.walk(cacheDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".spv"))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            LOGGER.warn("Failed to delete cached file: {}", path, e);
                        }
                    });

            LOGGER.info("Shader cache cleared");
        } catch (IOException e) {
            LOGGER.error("Failed to clear shader cache", e);
        }
    }

    /**
     * Get cache statistics
     */
    public CacheStats getStats() {
        int diskCacheSize = 0;
        long totalSizeBytes = 0;

        try {
            diskCacheSize = (int) Files.walk(cacheDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".spv"))
                    .count();

            totalSizeBytes = Files.walk(cacheDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".spv"))
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            LOGGER.warn("Failed to get cache stats", e);
        }

        return new CacheStats(memoryCache.size(), diskCacheSize, totalSizeBytes);
    }

    public static class CacheStats {
        public final int memoryCached;
        public final int diskCached;
        public final long totalSizeBytes;

        public CacheStats(int memoryCached, int diskCached, long totalSizeBytes) {
            this.memoryCached = memoryCached;
            this.diskCached = diskCached;
            this.totalSizeBytes = totalSizeBytes;
        }

        @Override
        public String toString() {
            return String.format("Memory: %d, Disk: %d, Size: %.2f MB",
                    memoryCached, diskCached, totalSizeBytes / 1024.0 / 1024.0);
        }
    }
}
