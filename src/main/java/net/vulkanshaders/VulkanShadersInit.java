package net.vulkanshaders;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.vulkanshaders.compiler.ShaderCache;
import net.vulkanshaders.compiler.SPIRVCompiler;
import net.vulkanshaders.loader.ShaderPackLoader;
import net.vulkanshaders.loader.ShaderPackRegistry;
import net.vulkanshaders.model.ShaderPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class VulkanShadersInit implements ModInitializer {
    public static final String MOD_ID = "vulkanshaders";
    public static final Logger LOGGER = LoggerFactory.getLogger("VulkanShaders");

    private static ShaderCache shaderCache;
    private static SPIRVCompiler spirvCompiler;

    @Override
    public void onInitialize() {
        LOGGER.info("VulkanShaders initializing...");

        // Check if VulkanMod is loaded
        if (!FabricLoader.getInstance().isModLoaded("vulkanmod")) {
            LOGGER.warn("VulkanMod not found! VulkanShaders requires VulkanMod to function.");
            LOGGER.warn("Shader loading will be disabled.");
            return;
        }

        // Initialize shader cache
        shaderCache = new ShaderCache();
        spirvCompiler = new SPIRVCompiler(shaderCache);
        LOGGER.info("Shader compiler initialized");

        // Create shaderpacks directory
        Path shaderpacksDir = FabricLoader.getInstance()
                .getGameDir().resolve("shaderpacks");
        try {
            Files.createDirectories(shaderpacksDir);
            LOGGER.info("Shader packs directory: {}", shaderpacksDir);
        } catch (Exception e) {
            LOGGER.error("Failed to create shaderpacks directory", e);
            return;
        }

        // Load shader packs
        LOGGER.info("Scanning for shader packs...");
        List<ShaderPack> packs = ShaderPackLoader.loadAllPacks();

        for (ShaderPack pack : packs) {
            ShaderPackRegistry.registerPack(pack);
            LOGGER.info("Registered shader pack: {} v{} by {}",
                    pack.getName(),
                    pack.getVersion(),
                    pack.getMetadata().author != null ? pack.getMetadata().author : "Unknown");
        }

        if (packs.isEmpty()) {
            LOGGER.info("No shader packs found in {}", shaderpacksDir);
            LOGGER.info("Place .zip shader packs in this directory to use custom shaders");
        } else {
            LOGGER.info("Loaded {} shader pack(s)", packs.size());

            // Log cache stats
            var stats = shaderCache.getStats();
            LOGGER.info("Shader cache: {}", stats);
        }

        LOGGER.info("VulkanShaders initialized successfully!");
    }

    public static ShaderCache getShaderCache() {
        return shaderCache;
    }

    public static SPIRVCompiler getCompiler() {
        return spirvCompiler;
    }
}
