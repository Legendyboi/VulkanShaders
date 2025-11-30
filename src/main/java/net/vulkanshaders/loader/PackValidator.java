package net.vulkanshaders.loader;

import net.vulkanshaders.model.ShaderPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates shader pack structure and compatibility
 */
public class PackValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger("VulkanShaders/Validator");

    public static boolean validate(ShaderPack pack) {
        if (pack == null) {
            LOGGER.error("Shader pack is null");
            return false;
        }

        var metadata = pack.getMetadata();

        // Check required fields
        if (metadata.name == null || metadata.name.isEmpty()) {
            LOGGER.error("Shader pack missing name");
            return false;
        }

        if (metadata.version == null || metadata.version.isEmpty()) {
            LOGGER.error("Shader pack {} missing version", metadata.name);
            return false;
        }

        if (metadata.pipelines == null || metadata.pipelines.isEmpty()) {
            LOGGER.error("Shader pack {} has no pipelines defined", metadata.name);
            return false;
        }

        // Validate each pipeline
        for (var entry : metadata.pipelines.entrySet()) {
            String name = entry.getKey();
            var pipeline = entry.getValue();

            if (pipeline.vertex == null && pipeline.fragment == null) {
                LOGGER.error("Pipeline {} in pack {} has no shaders",
                        name, metadata.name);
                return false;
            }

            // Check if shader sources exist
            if (pipeline.vertex != null && pack.getShaderSource(pipeline.vertex) == null) {
                LOGGER.error("Missing vertex shader: {}", pipeline.vertex);
                return false;
            }

            if (pipeline.fragment != null && pack.getShaderSource(pipeline.fragment) == null) {
                LOGGER.error("Missing fragment shader: {}", pipeline.fragment);
                return false;
            }
        }

        LOGGER.info("Shader pack {} validated successfully", metadata.name);
        return true;
    }
}
