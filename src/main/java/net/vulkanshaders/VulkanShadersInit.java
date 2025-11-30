package net.vulkanshaders;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.vulkanmod.vulkan.shader.SPIRVUtils;
import net.vulkanshaders.compiler.ShaderCache;
import net.vulkanshaders.compiler.SPIRVCompiler;
import net.vulkanshaders.loader.ShaderPackLoader;
import net.vulkanshaders.loader.ShaderPackRegistry;
import net.vulkanshaders.model.ShaderPack;
import net.vulkanshaders.pipeline.CustomPipeline;
import net.vulkanshaders.pipeline.PipelineConfiguration;
import net.vulkanshaders.pipeline.PipelineManager;
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

            // Load and compile pipelines from pack
            loadPipelinesFromPack(pack);
        }

        if (packs.isEmpty()) {
            LOGGER.info("No shader packs found in {}", shaderpacksDir);
            LOGGER.info("Place .zip shader packs in this directory to use custom shaders");
        } else {
            LOGGER.info("Loaded {} shader pack(s)", packs.size());
            LOGGER.info(PipelineManager.getStats());

            // Setup pipeline overrides
            PipelineManager.getPipeline("terrain").ifPresent(pipeline -> {
                PipelineManager.setOverride("terrain", "terrain");
            });

            PipelineManager.setOverridesEnabled(true);
            LOGGER.info("Custom shader pipelines registered (will initialize when Vulkan is ready)");

            // Log cache stats
            var stats = shaderCache.getStats();
            LOGGER.info("Shader cache: {}", stats);
        }

        LOGGER.info("VulkanShaders initialized successfully!");
    }

    private void loadPipelinesFromPack(ShaderPack pack) {
        LOGGER.info("Loading pipelines from pack: {}", pack.getName());

        if (pack.getMetadata().pipelines == null || pack.getMetadata().pipelines.isEmpty()) {
            LOGGER.warn("No pipelines defined in pack.json for {}", pack.getName());
            return;
        }

        int loaded = 0;
        int compiled = 0;
        int registered = 0;

        for (var entry : pack.getMetadata().pipelines.entrySet()) {
            String pipelineName = entry.getKey();
            var pipelineConfig = entry.getValue();

            try {
                // Get shader sources
                String vertSource = pack.getAllShaderSources().get(pipelineConfig.vertex);
                String fragSource = pack.getAllShaderSources().get(pipelineConfig.fragment);

                if (vertSource == null || fragSource == null) {
                    LOGGER.error("Missing shader sources for pipeline: {}", pipelineName);
                    continue;
                }

                LOGGER.info("Found shader program: {}", pipelineName);
                LOGGER.info("  Vertex: {} ({} bytes)", pipelineConfig.vertex, vertSource.length());
                LOGGER.info("  Fragment: {} ({} bytes)", pipelineConfig.fragment, fragSource.length());

                // Compile shaders
                try {
                    LOGGER.info("Compiling shaders for pipeline: {}", pipelineName);

                    var VERTEX_SHADER = SPIRVUtils.ShaderKind.VERTEX_SHADER;
                    var FRAGMENT_SHADER = SPIRVUtils.ShaderKind.FRAGMENT_SHADER;

                    // Compile vertex shader
                    var vertSpirv = spirvCompiler.compile(
                            pipelineConfig.vertex,
                            vertSource,
                            VERTEX_SHADER,
                            pack.getVersion()
                    );

                    LOGGER.info("  ✓ Vertex shader compiled ({} bytes SPIR-V)",
                            vertSpirv.bytecode().remaining());

                    // Compile fragment shader
                    var fragSpirv = spirvCompiler.compile(
                            pipelineConfig.fragment,
                            fragSource,
                            FRAGMENT_SHADER,
                            pack.getVersion()
                    );

                    LOGGER.info("  ✓ Fragment shader compiled ({} bytes SPIR-V)",
                            fragSpirv.bytecode().remaining());

                    compiled++;

                    // Create PipelineConfiguration from PipelineConfig (JSON)
                    PipelineConfiguration configuration =
                            PipelineConfiguration.fromPipelineConfig(pipelineConfig);

                    // Create CustomPipeline
                    CustomPipeline customPipeline = new CustomPipeline(
                            pipelineName,
                            pack,
                            vertSpirv,
                            fragSpirv,
                            configuration
                    );

                    // Register but DON'T initialize yet (Vulkan context not ready)
                    PipelineManager.registerPipeline(pipelineName, customPipeline);
                    LOGGER.info("  ✓ Registered custom pipeline: {}", pipelineName);
                    registered++;

                } catch (Exception e) {
                    LOGGER.error("Failed to compile shaders for pipeline: {}", pipelineName, e);
                    continue;
                }

                loaded++;
            } catch (Exception e) {
                LOGGER.error("Failed to load pipeline: {}", pipelineName, e);
            }
        }

        LOGGER.info("Found {} shader programs in {} ({} compiled, {} registered)",
                loaded, pack.getName(), compiled, registered);
    }

    public static ShaderCache getShaderCache() {
        return shaderCache;
    }

    public static SPIRVCompiler getCompiler() {
        return spirvCompiler;
    }
}
