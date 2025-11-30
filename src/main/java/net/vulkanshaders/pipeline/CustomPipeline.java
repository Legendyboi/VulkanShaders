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
     * Initialize the Vulkan pipeline by copying from a VulkanMod pipeline
     * This must be called AFTER VulkanMod has created its pipelines
     */
    public void initializeFrom(GraphicsPipeline templatePipeline) {
        if (initialized) {
            LOGGER.warn("Pipeline {} already initialized", name);
            return;
        }

        try {
            LOGGER.info("Initializing custom pipeline: {} from pack {}", name, sourcePack.getName());

            // Get vertex format from config or use default
            VertexFormat vertexFormat = config.getVertexFormat() != null ?
                    config.getVertexFormat() : DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP;

            // Build VulkanMod Pipeline using the Builder class
            Pipeline.Builder builder = new Pipeline.Builder(vertexFormat, name + "_custom");

            // Set the compiled SPIR-V shaders
            builder.setSPIRVs(vertexShader, fragmentShader);

            // Copy UBOs and descriptors from template pipeline
            // The builder will auto-discover from SPIR-V
            builder.setUniforms(
                    java.util.Collections.emptyList(),
                    java.util.Collections.emptyList()
            );

            // Create the graphics pipeline
            this.vulkanPipeline = builder.createGraphicsPipeline();
            this.initialized = true;

            LOGGER.info("✓ Custom pipeline '{}' initialized successfully", name);

        } catch (Exception e) {
            LOGGER.error("✗ Failed to initialize custom pipeline: {}", name, e);
            throw new RuntimeException("Pipeline initialization failed: " + name, e);
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
