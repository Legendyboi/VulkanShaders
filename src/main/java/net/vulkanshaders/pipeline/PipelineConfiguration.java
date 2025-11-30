package net.vulkanshaders.pipeline;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PipelineConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger("VulkanShaders/PipelineConfig");

    private VertexFormat vertexFormat;
    private BlendMode blendMode;
    private DepthTest depthTest;
    private CullMode cullMode;
    private List<UBO> uniformBuffers = new ArrayList<>();
    private List<ImageDescriptor> imageDescriptors = new ArrayList<>();

    /**
     * Create configuration from shader pack PipelineConfig (from pack.json)
     */
    public static PipelineConfiguration fromPipelineConfig(net.vulkanshaders.model.PipelineConfig config) {
        PipelineConfiguration pipelineConfig = new PipelineConfiguration();

        // Parse blend mode from config
        pipelineConfig.blendMode = parseBlendMode(config.blend);

        // Parse depth test
        pipelineConfig.depthTest = config.depthTest ? DepthTest.LEQUAL : DepthTest.ALWAYS;

        // Parse cull mode
        pipelineConfig.cullMode = parseCullMode(config.cullFace);

        // Initialize defaults
        initializeDefaults(pipelineConfig);

        LOGGER.debug("Created pipeline config from PipelineConfig: blend={}, depth={}, cull={}",
                pipelineConfig.blendMode, pipelineConfig.depthTest, pipelineConfig.cullMode);

        return pipelineConfig;
    }

    /**
     * Parse configuration from shader pack properties file
     */
    public static PipelineConfiguration fromShaderProperties(Properties props) {
        PipelineConfiguration config = new PipelineConfiguration();

        // Parse blend mode
        String blend = props.getProperty("blend", "off");
        config.blendMode = parseBlendMode(blend);

        // Parse depth test
        String depthTest = props.getProperty("depthTest", "lequal");
        config.depthTest = parseDepthTest(depthTest);

        // Parse cull mode
        String cull = props.getProperty("cull", "back");
        config.cullMode = parseCullMode(cull);

        LOGGER.debug("Parsed pipeline config: blend={}, depth={}, cull={}",
                config.blendMode, config.depthTest, config.cullMode);

        return config;
    }

    /**
     * Initialize default descriptors for Minecraft rendering
     */
    private static void initializeDefaults(PipelineConfiguration config) {
        // The Pipeline.Builder will automatically create UBOs and descriptors
        // based on the SPIR-V shader bindings
        LOGGER.debug("Configuration ready for pipeline builder");
    }

    /**
     * Apply this configuration to a Pipeline.Builder
     */
    public void applyTo(Pipeline.Builder builder) {
        // Apply blend mode
        if (blendMode != null) {
            // TODO: Set blend state on builder when API is available
        }

        // Apply depth test
        if (depthTest != null) {
            // TODO: Set depth test on builder when API is available
        }

        // Apply cull mode
        if (cullMode != null) {
            // TODO: Set cull mode on builder when API is available
        }

        // Note: UBOs and ImageDescriptors are set via builder.setUniforms()
    }

    private static BlendMode parseBlendMode(String value) {
        return switch (value.toLowerCase()) {
            case "off", "opaque" -> BlendMode.OFF;
            case "add" -> BlendMode.ADD;
            case "alpha", "translucent" -> BlendMode.ALPHA;
            case "multiply" -> BlendMode.MULTIPLY;
            default -> {
                LOGGER.warn("Unknown blend mode: {}, using OFF", value);
                yield BlendMode.OFF;
            }
        };
    }

    private static DepthTest parseDepthTest(String value) {
        return switch (value.toLowerCase()) {
            case "never" -> DepthTest.NEVER;
            case "less" -> DepthTest.LESS;
            case "equal" -> DepthTest.EQUAL;
            case "lequal" -> DepthTest.LEQUAL;
            case "greater" -> DepthTest.GREATER;
            case "notequal" -> DepthTest.NOTEQUAL;
            case "gequal" -> DepthTest.GEQUAL;
            case "always" -> DepthTest.ALWAYS;
            default -> {
                LOGGER.warn("Unknown depth test: {}, using LEQUAL", value);
                yield DepthTest.LEQUAL;
            }
        };
    }

    private static CullMode parseCullMode(String value) {
        return switch (value.toLowerCase()) {
            case "off", "none" -> CullMode.NONE;
            case "back" -> CullMode.BACK;
            case "front" -> CullMode.FRONT;
            default -> {
                LOGGER.warn("Unknown cull mode: {}, using BACK", value);
                yield CullMode.BACK;
            }
        };
    }

    // Getters and setters
    public VertexFormat getVertexFormat() {
        return vertexFormat;
    }

    public void setVertexFormat(VertexFormat vertexFormat) {
        this.vertexFormat = vertexFormat;
    }

    public BlendMode getBlendMode() {
        return blendMode;
    }

    public DepthTest getDepthTest() {
        return depthTest;
    }

    public CullMode getCullMode() {
        return cullMode;
    }

    public List<UBO> getUniformBuffers() {
        return uniformBuffers;
    }

    public List<ImageDescriptor> getImageDescriptors() {
        return imageDescriptors;
    }

    // Configuration enums
    public enum BlendMode {
        OFF, ADD, ALPHA, MULTIPLY
    }

    public enum DepthTest {
        NEVER, LESS, EQUAL, LEQUAL, GREATER, NOTEQUAL, GEQUAL, ALWAYS
    }

    public enum CullMode {
        NONE, BACK, FRONT
    }
}
