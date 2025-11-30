package net.vulkanshaders.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages custom shader pipelines and their overrides
 */
public class PipelineManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("VulkanShaders/Pipeline");

    // Map of pipeline name -> CustomPipeline
    private static final Map<String, CustomPipeline> CUSTOM_PIPELINES = new HashMap<>();

    // Map of VulkanMod pipeline name -> CustomPipeline (for overrides)
    private static final Map<String, CustomPipeline> PIPELINE_OVERRIDES = new HashMap<>();

    private static boolean overridesEnabled = false;
    private static boolean pipelinesInitialized = false;

    /**
     * Register a custom pipeline
     */
    public static void registerPipeline(String name, CustomPipeline pipeline) {
        CUSTOM_PIPELINES.put(name, pipeline);
        LOGGER.debug("Registered custom pipeline: {}", name);
    }

    /**
     * Initialize all registered pipelines (call after Vulkan context is ready)
     */
    public static void initializeAll() {
        LOGGER.warn("Pipeline initialization is deferred - will be applied during rendering");
        pipelinesInitialized = true;
        LOGGER.info("Marked {} custom pipelines as ready", CUSTOM_PIPELINES.size());
    }

    /**
     * Set a custom pipeline to override a VulkanMod pipeline
     */
    public static void setOverride(String vulkanModPipelineName, String customPipelineName) {
        CustomPipeline pipeline = CUSTOM_PIPELINES.get(customPipelineName);
        if (pipeline == null) {
            LOGGER.warn("Cannot override {}: custom pipeline {} not found",
                    vulkanModPipelineName, customPipelineName);
            return;
        }

        PIPELINE_OVERRIDES.put(vulkanModPipelineName, pipeline);
        LOGGER.info("Set override: {} -> {}", vulkanModPipelineName, customPipelineName);
    }

    /**
     * Get the override pipeline for a VulkanMod pipeline
     */
    public static Optional<CustomPipeline> getOverride(String vulkanModPipelineName) {
        if (!overridesEnabled || !pipelinesInitialized) {
            return Optional.empty();
        }
        return Optional.ofNullable(PIPELINE_OVERRIDES.get(vulkanModPipelineName));
    }

    /**
     * Get a custom pipeline by name
     */
    public static Optional<CustomPipeline> getPipeline(String name) {
        return Optional.ofNullable(CUSTOM_PIPELINES.get(name));
    }

    /**
     * Enable or disable pipeline overrides
     */
    public static void setOverridesEnabled(boolean enabled) {
        overridesEnabled = enabled;
        LOGGER.info("Pipeline overrides: {}", enabled ? "enabled" : "disabled");
    }

    public static boolean areOverridesEnabled() {
        return overridesEnabled;
    }

    public static boolean arePipelinesInitialized() {
        return pipelinesInitialized;
    }

    /**
     * Get statistics
     */
    public static String getStats() {
        return String.format("Registered %d custom pipelines (%d overrides)",
                CUSTOM_PIPELINES.size(),
                PIPELINE_OVERRIDES.size());
    }

    /**
     * Clear all pipelines (for reload)
     */
    public static void clear() {
        CUSTOM_PIPELINES.values().forEach(CustomPipeline::cleanup);
        CUSTOM_PIPELINES.clear();
        PIPELINE_OVERRIDES.clear();
        overridesEnabled = false;
        pipelinesInitialized = false;
    }
}
