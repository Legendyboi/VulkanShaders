package net.vulkanshaders.loader;

import net.vulkanshaders.model.ShaderPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Registry for managing loaded shader packs
 */
public class ShaderPackRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("VulkanShaders/Registry");
    private static final List<ShaderPack> LOADED_PACKS = new ArrayList<>();
    private static ShaderPack activePack = null;

    public static void registerPack(ShaderPack pack) {
        LOADED_PACKS.add(pack);
        LOGGER.info("Registered shader pack: {}", pack.getName());
    }

    public static List<ShaderPack> getAllPacks() {
        return new ArrayList<>(LOADED_PACKS);
    }

    public static Optional<ShaderPack> getPack(String name) {
        return LOADED_PACKS.stream()
                .filter(pack -> pack.getName().equals(name))
                .findFirst();
    }

    public static void setActivePack(ShaderPack pack) {
        if (activePack != null) {
            LOGGER.info("Deactivating shader pack: {}", activePack.getName());
        }

        activePack = pack;

        if (pack != null) {
            LOGGER.info("Activated shader pack: {}", pack.getName());
        }
    }

    public static ShaderPack getActivePack() {
        return activePack;
    }

    public static void clear() {
        LOADED_PACKS.clear();
        activePack = null;
    }
}
