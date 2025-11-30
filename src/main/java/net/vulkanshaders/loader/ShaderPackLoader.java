package net.vulkanshaders.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.vulkanshaders.model.PackMetadata;
import net.vulkanshaders.model.ShaderPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loads shader packs from the shaderpacks directory
 */
public class ShaderPackLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("VulkanShaders/Loader");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path SHADER_PACKS_DIR = FabricLoader.getInstance()
            .getGameDir().resolve("shaderpacks");

    /**
     * Load all shader packs from the shaderpacks directory
     */
    public static List<ShaderPack> loadAllPacks() {
        List<ShaderPack> packs = new ArrayList<>();

        // Ensure directory exists
        try {
            Files.createDirectories(SHADER_PACKS_DIR);
        } catch (IOException e) {
            LOGGER.error("Failed to create shaderpacks directory", e);
            return packs;
        }

        try (Stream<Path> paths = Files.list(SHADER_PACKS_DIR)) {
            paths.filter(p -> p.toString().endsWith(".zip"))
                    .forEach(zipPath -> {
                        try {
                            ShaderPack pack = loadPack(zipPath);
                            if (PackValidator.validate(pack)) {
                                packs.add(pack);
                                LOGGER.info("Loaded shader pack: {} v{}",
                                        pack.getName(), pack.getVersion());
                            }
                        } catch (Exception e) {
                            LOGGER.error("Failed to load shader pack: {}",
                                    zipPath.getFileName(), e);
                        }
                    });
        } catch (IOException e) {
            LOGGER.error("Failed to list shader packs", e);
        }

        return packs;
    }

    private static ShaderPack loadPack(Path zipPath) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(zipPath, (ClassLoader) null)) {
            // Load pack.json
            Path packJsonPath = fs.getPath("pack.json");
            if (!Files.exists(packJsonPath)) {
                throw new IOException("pack.json not found in shader pack");
            }

            String packJson = Files.readString(packJsonPath);
            PackMetadata metadata = GSON.fromJson(packJson, PackMetadata.class);

            // Load all shader sources
            Map<String, String> shaderSources = new HashMap<>();
            loadShaderSources(fs, metadata, shaderSources);

            return new ShaderPack(metadata, shaderSources, zipPath);
        }
    }

    private static void loadShaderSources(FileSystem fs, PackMetadata metadata,
                                          Map<String, String> sources) throws IOException {
        // Load pipeline shaders
        for (var pipeline : metadata.pipelines.values()) {
            if (pipeline.vertex != null) {
                loadShaderFile(fs, pipeline.vertex, sources);
            }
            if (pipeline.fragment != null) {
                loadShaderFile(fs, pipeline.fragment, sources);
            }
            if (pipeline.geometry != null) {
                loadShaderFile(fs, pipeline.geometry, sources);
            }

            // Load includes
            for (String include : pipeline.includes) {
                loadShaderFile(fs, include, sources);
            }
        }
    }

    private static void loadShaderFile(FileSystem fs, String path,
                                       Map<String, String> sources) throws IOException {
        if (sources.containsKey(path)) {
            return; // Already loaded
        }

        Path shaderPath = fs.getPath(path);
        if (!Files.exists(shaderPath)) {
            throw new IOException("Shader file not found: " + path);
        }

        String content = Files.readString(shaderPath);
        sources.put(path, content);
    }
}
