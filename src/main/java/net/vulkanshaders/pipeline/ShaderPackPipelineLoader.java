package net.vulkanshaders.pipeline;

import net.vulkanmod.vulkan.shader.SPIRVUtils;
import net.vulkanshaders.VulkanShadersInit;
import net.vulkanshaders.compiler.GLSLPreprocessor;
import net.vulkanshaders.model.ShaderPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class ShaderPackPipelineLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("VulkanShaders/PipelineLoader");

    /**
     * Load and register all pipelines from a shader pack
     */
    public static void loadPipelinesFromPack(ShaderPack pack) {
        LOGGER.info("Loading pipelines from pack: {}", pack.getName());

        try {
            // Discover shader programs in the pack
            List<ShaderProgram> programs = discoverPrograms(pack);

            LOGGER.info("Found {} shader programs in {}", programs.size(), pack.getName());

            for (ShaderProgram program : programs) {
                try {
                    loadProgram(pack, program);
                } catch (Exception e) {
                    LOGGER.error("Failed to load shader program: {}", program.getName(), e);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to load pipelines from pack: {}", pack.getName(), e);
        }
    }

    private static void loadProgram(ShaderPack pack, ShaderProgram program) {
        LOGGER.info("Loading shader program: {}", program.getName());

        // Create preprocessor with available includes from the pack
        GLSLPreprocessor preprocessor = new GLSLPreprocessor(pack.getAllShaderSources());

        // Preprocess vertex shader
        String processedVert = preprocessor.preprocess(
                program.getVertexSource(),
                "shaders/" + program.getName() + ".vsh"
        );

        // Preprocess fragment shader
        String processedFrag = preprocessor.preprocess(
                program.getFragmentSource(),
                "shaders/" + program.getName() + ".fsh"
        );

        // Compile to SPIR-V
        SPIRVUtils.SPIRV vertSpirv = VulkanShadersInit.getCompiler().compile(
                program.getName() + ".vert",
                processedVert,
                SPIRVUtils.ShaderKind.VERTEX_SHADER,
                pack.getVersion()
        );

        SPIRVUtils.SPIRV fragSpirv = VulkanShadersInit.getCompiler().compile(
                program.getName() + ".frag",
                processedFrag,
                SPIRVUtils.ShaderKind.FRAGMENT_SHADER,
                pack.getVersion()
        );

        // Create configuration from properties
        PipelineConfiguration config = PipelineConfiguration.fromShaderProperties(
                program.getProperties()
        );

        // Build custom pipeline
        CustomPipeline pipeline = new CustomPipeline(
                program.getName(),
                pack,
                vertSpirv,
                fragSpirv,
                config
        );

        // Register the pipeline
        PipelineRegistry.registerCustomPipeline(program.getName(), pipeline);

        // Register override if applicable
        if (program.isOverride()) {
            PipelineRegistry.registerOverride(
                    program.getVanillaTarget(),
                    new PipelineOverride(pipeline)
            );
            LOGGER.info("Registered override: {} -> {}",
                    program.getVanillaTarget(), program.getName());
        }
    }

    /**
     * Discover shader programs in a shader pack
     */
    private static List<ShaderProgram> discoverPrograms(ShaderPack pack) {
        List<ShaderProgram> programs = new ArrayList<>();

        // Standard shader program names (based on OptiFine/Iris format)
        String[] programNames = {
                "gbuffers_basic",
                "gbuffers_textured",
                "gbuffers_textured_lit",
                "gbuffers_terrain",
                "gbuffers_damagedblock",
                "gbuffers_skybasic",
                "gbuffers_skytextured",
                "gbuffers_clouds",
                "gbuffers_weather",
                "gbuffers_entities",
                "gbuffers_armor_glint",
                "gbuffers_spidereyes",
                "gbuffers_hand",
                "gbuffers_hand_water",
                "composite",
                "final"
        };

        for (String programName : programNames) {
            try {
                ShaderProgram program = loadProgramIfExists(pack, programName);
                if (program != null) {
                    programs.add(program);
                }
            } catch (Exception e) {
                LOGGER.debug("Program {} not found in pack {}", programName, pack.getName());
            }
        }

        return programs;
    }

    private static ShaderProgram loadProgramIfExists(ShaderPack pack, String programName) {
        // Try to load vertex shader
        String vertPath = "shaders/" + programName + ".vsh";
        String vertSource = pack.getShaderSource(vertPath);
        if (vertSource == null) {
            return null; // Vertex shader not found
        }

        // Try to load fragment shader
        String fragPath = "shaders/" + programName + ".fsh";
        String fragSource = pack.getShaderSource(fragPath);
        if (fragSource == null) {
            return null; // Fragment shader not found
        }

        // Load properties file (optional)
        Properties props = new Properties();
        String propsPath = "shaders/" + programName + ".properties";
        String propsSource = pack.getShaderSource(propsPath);
        if (propsSource != null) {
            try (InputStream is = new ByteArrayInputStream(propsSource.getBytes(StandardCharsets.UTF_8))) {
                props.load(is);
            } catch (IOException e) {
                LOGGER.warn("Failed to load properties for {}", programName, e);
            }
        }

        // Check if this is an override
        boolean isOverride = props.getProperty("override", "false").equalsIgnoreCase("true");
        String vanillaTarget = props.getProperty("vanillaTarget", programName);

        return new ShaderProgram(
                programName,
                vertSource,
                fragSource,
                props,
                isOverride,
                vanillaTarget
        );
    }
}
