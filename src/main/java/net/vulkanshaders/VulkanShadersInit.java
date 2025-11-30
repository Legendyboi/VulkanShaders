package net.vulkanshaders;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class VulkanShadersInit implements ModInitializer {
    public static final String MOD_ID = "vulkanshaders";
    public static final Logger LOGGER = LoggerFactory.getLogger("VulkanShaders");

    @Override
    public void onInitialize() {
        LOGGER.info("VulkanShaders initializing...");

        // Check if VulkanMod is loaded
        if (!FabricLoader.getInstance().isModLoaded("vulkanmod")) {
            LOGGER.warn("VulkanMod not found! VulkanShaders requires VulkanMod to function.");
            LOGGER.warn("Shader loading will be disabled.");
            return;
        }

        // Create shaderpacks directory
        Path shaderpacksDir = FabricLoader.getInstance()
                .getGameDir().resolve("shaderpacks");
        try {
            Files.createDirectories(shaderpacksDir);
            LOGGER.info("Shader packs directory: {}", shaderpacksDir);
        } catch (Exception e) {
            LOGGER.error("Failed to create shaderpacks directory", e);
        }

        LOGGER.info("VulkanShaders initialized successfully!");
    }
}
