package net.vulkanshaders.pipeline;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.SPIRVUtils;
import net.vulkanshaders.model.ShaderPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomPipeline {
    private static final Logger LOGGER = LoggerFactory.getLogger("VulkanShaders/CustomPipeline");

    private final String name;
    private final ShaderPack sourcePack;
    private final SPIRVUtils.SPIRV vertexShader;
    private final SPIRVUtils.SPIRV fragmentShader;
    private final PipelineConfiguration config;

    // Lazily initialized VulkanMod GraphicsPipeline
    private GraphicsPipeline vulkanPipeline;
    private boolean initialized = false;

    public CustomPipeline(String name, ShaderPack sourcePack,
                          SPIRVUtils.SPIRV vertexShader,
                          SPIRVUtils.SPIRV fragmentShader,
                          PipelineConfiguration config) {
        this.name = name;
        this.sourcePack = sourcePack;
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.config = config;
    }

    /**
     * Initialize the Vulkan pipeline (call after Vulkan context is ready)
     * WARNING: This can crash if called too early!
     */
    public void initialize() {
        if (initialized) {
            LOGGER.warn("Pipeline {} already initialized", name);
            return;
        }

        try {
            LOGGER.info("Initializing pipeline: {} from pack {}", name, sourcePack.getName());

            // Get vertex format from config or use default
            VertexFormat vertexFormat = config.getVertexFormat() != null ?
                    config.getVertexFormat() : DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP;

            // SAFETY CHECK: Don't actually create the pipeline yet
            // Just mark as "ready to initialize"
            this.initialized = false; // Keep false for now

            LOGGER.info("Pipeline {} prepared (deferred initialization)", name);

            // TODO: Actually create the pipeline when it's safe
            // This requires hooking into VulkanMod's pipeline creation flow

        } catch (Exception e) {
            LOGGER.error("Failed to prepare pipeline: {}", name, e);
            throw new RuntimeException("Pipeline preparation failed: " + name, e);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public GraphicsPipeline getVulkanPipeline() {
        if (!initialized) {
            throw new IllegalStateException("Pipeline not initialized: " + name);
        }
        return vulkanPipeline;
    }

    public String getName() {
        return name;
    }

    public ShaderPack getSourcePack() {
        return sourcePack;
    }

    public PipelineConfiguration getConfig() {
        return config;
    }

    public SPIRVUtils.SPIRV getVertexShader() {
        return vertexShader;
    }

    public SPIRVUtils.SPIRV getFragmentShader() {
        return fragmentShader;
    }

    /**
     * Cleanup Vulkan resources
     */
    public void cleanup() {
        if (vulkanPipeline != null) {
            vulkanPipeline.cleanUp();
            initialized = false;
            LOGGER.debug("Cleaned up pipeline: {}", name);
        }
    }
}
