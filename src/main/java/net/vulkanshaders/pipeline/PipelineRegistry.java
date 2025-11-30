package net.vulkanshaders.pipeline;

import net.vulkanshaders.VulkanShadersInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PipelineRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("VulkanShaders/Pipeline");

    private static final Map<String, CustomPipeline> registeredPipelines = new ConcurrentHashMap<>();
    private static final Map<String, PipelineOverride> overrides = new ConcurrentHashMap<>();

    /**
     * Register a custom pipeline from a shader pack
     */
    public static void registerCustomPipeline(String name, CustomPipeline pipeline) {
        registeredPipelines.put(name, pipeline);
        LOGGER.info("Registered custom pipeline: {}", name);
    }

    /**
     * Register an override for a vanilla VulkanMod pipeline
     */
    public static void registerOverride(String vanillaPipelineName, PipelineOverride override) {
        overrides.put(vanillaPipelineName, override);
        LOGGER.info("Registered override for vanilla pipeline: {}", vanillaPipelineName);
    }

    /**
     * Get a custom pipeline by name
     */
    public static Optional<CustomPipeline> getPipeline(String name) {
        return Optional.ofNullable(registeredPipelines.get(name));
    }

    /**
     * Get an override for a vanilla pipeline
     */
    public static Optional<PipelineOverride> getOverride(String vanillaPipelineName) {
        return Optional.ofNullable(overrides.get(vanillaPipelineName));
    }

    /**
     * Clear all registered pipelines (for hot reload)
     */
    public static void clear() {
        registeredPipelines.values().forEach(CustomPipeline::cleanup);
        registeredPipelines.clear();
        overrides.clear();
        LOGGER.info("Cleared all registered pipelines");
    }

    /**
     * Get total number of registered pipelines
     */
    public static int getPipelineCount() {
        return registeredPipelines.size();
    }

    /**
     * Get total number of overrides
     */
    public static int getOverrideCount() {
        return overrides.size();
    }
}
