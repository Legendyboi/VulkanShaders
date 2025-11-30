package net.vulkanshaders.compiler;

import net.vulkanmod.vulkan.shader.SPIRVUtils;
import net.vulkanmod.vulkan.shader.converter.GLSLParser;
import net.vulkanmod.vulkan.shader.converter.Lexer;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compiles GLSL 450 shader source to SPIR-V bytecode with caching
 * Supports both Vulkan-native and OpenGL-style GLSL (auto-converted via VulkanMod)
 */
public class SPIRVCompiler {
    private static final Logger LOGGER = LoggerFactory.getLogger("VulkanShaders/Compiler");
    private final ShaderCache cache;
    private final Map<String, ShaderMetadata> metadataCache = new HashMap<>();

    public SPIRVCompiler(ShaderCache cache) {
        this.cache = cache;
    }

    public SPIRVUtils.SPIRV compile(String shaderName, String source,
                                    SPIRVUtils.ShaderKind kind,
                                    String packVersion) {
        // CRITICAL: Preprocess FIRST, use converted source for cache key
        String vulkanSource = preprocessGLSL(shaderName, source, kind);
        String cacheKey = generateCacheKey(shaderName, vulkanSource, kind, packVersion);

        ByteBuffer cached = cache.get(cacheKey);
        if (cached != null) {
            LOGGER.debug("Loaded {} from cache", shaderName);
            return new SPIRVUtils.SPIRV(0, cached);
        }

        LOGGER.info("Compiling shader: {} ({}). Source: {} chars",
                shaderName, kind, vulkanSource.length());

        long startTime = System.currentTimeMillis();
        SPIRVUtils.SPIRV spirv;
        try {
            spirv = SPIRVUtils.compileShader(shaderName, vulkanSource, kind);
        } catch (Exception e) {
            LOGGER.error("SPIR-V compilation failed: {}", shaderName, e);
            throw new RuntimeException("Shader compilation failed: " + shaderName, e);
        }

        long duration = System.currentTimeMillis() - startTime;
        LOGGER.info("Compiled {} in {}ms", shaderName, duration);

        cache.put(cacheKey, spirv.bytecode());
        return spirv;
    }

    private String preprocessGLSL(String shaderName, String source, SPIRVUtils.ShaderKind kind) {
        if (!needsConversion(source)) {
            LOGGER.debug("{}: Native Vulkan GLSL (fast path)", shaderName);
            return source;
        }

        LOGGER.info("Converting OpenGL GLSL → Vulkan: {}", shaderName);
        return convertToVulkan(source, kind, shaderName);
    }

    private boolean needsConversion(String source) {
        String lower = source.toLowerCase();
        boolean hasGLSL = lower.contains("uniform ") ||
                (lower.contains("sampler2d") || lower.contains("samplercube"));
        boolean hasVulkan = lower.contains("layout(binding") || lower.contains("layout(location");
        return hasGLSL && !hasVulkan;
    }

    /** EXACT VulkanMod GLSLParser workflow from attached source */
    private String convertToVulkan(String source, SPIRVUtils.ShaderKind kind, String shaderName) {
        try {
            GLSLParser.Stage stage = switch (kind) {
                case VERTEX_SHADER -> GLSLParser.Stage.VERTEX;
                case FRAGMENT_SHADER -> GLSLParser.Stage.FRAGMENT;
                default -> throw new IllegalArgumentException("Unsupported: " + kind);
            };

            // VulkanMod exact sequence: Lexer → GLSLParser(lexer, stage) → parse()
            Lexer lexer = new Lexer(source);
            GLSLParser parser = new GLSLParser();
            parser.parse(lexer, stage);  // Tokenizes, parses uniforms/attributes/samplers

            // Get converted Vulkan GLSL for this stage
            String vulkanGLSL = parser.getOutput(stage);

            // Extract metadata (non-blocking for terrain pipeline)
            UBO[] ubos = parser.createUBOs();
            List<ImageDescriptor> samplers = parser.getSamplerList();

            metadataCache.put(shaderName, new ShaderMetadata(ubos, samplers));
            LOGGER.debug("{}: Converted → {} UBOs, {} samplers",
                    shaderName, ubos.length, samplers.size());

            return vulkanGLSL;

        } catch (Exception e) {
            LOGGER.error("GLSL conversion failed for {}: {}", shaderName, e.getMessage());
            LOGGER.debug("Source preview:\n{}", source.substring(0, Math.min(1000, source.length())));
            return source;  // Fallback: let SPIRVUtils fail-fast
        }
    }

    public ShaderMetadata getMetadata(String shaderName) {
        return metadataCache.get(shaderName);
    }

    private String generateCacheKey(String name, String source, SPIRVUtils.ShaderKind kind, String version) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(name.getBytes());
            digest.update(source.getBytes());  // POST-conversion source
            digest.update(kind.name().getBytes());
            digest.update(version.getBytes());

            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

    public static class ShaderMetadata {
        public final UBO[] ubos;
        public final List<ImageDescriptor> samplers;

        public ShaderMetadata(UBO[] ubos, List<ImageDescriptor> samplers) {
            this.ubos = ubos;
            this.samplers = samplers;
        }
    }
}
