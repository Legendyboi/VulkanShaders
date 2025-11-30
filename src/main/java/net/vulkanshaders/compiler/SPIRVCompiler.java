package net.vulkanshaders.compiler;

import net.vulkanmod.vulkan.shader.SPIRVUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Compiles GLSL 450 shader source to SPIR-V bytecode with caching
 */
public class SPIRVCompiler {
    private static final Logger LOGGER = LoggerFactory.getLogger("VulkanShaders/Compiler");
    private final ShaderCache cache;

    public SPIRVCompiler(ShaderCache cache) {
        this.cache = cache;
    }

    /**
     * Compile GLSL shader source to SPIR-V
     *
     * @param shaderName Shader name (for logging and caching)
     * @param source GLSL 450 source code (preprocessed)
     * @param kind Shader type (vertex, fragment, etc.)
     * @param packVersion Shader pack version (for cache invalidation)
     * @return Compiled SPIR-V bytecode
     */
    public SPIRVUtils.SPIRV compile(String shaderName, String source,
                                    SPIRVUtils.ShaderKind kind,
                                    String packVersion) {
        // Generate cache key
        String cacheKey = generateCacheKey(shaderName, source, kind, packVersion);

        // Try to load from cache
        ByteBuffer cached = cache.get(cacheKey);
        if (cached != null) {
            LOGGER.debug("Loaded {} from cache", shaderName);
            // Note: We're reusing VulkanMod's SPIRV class, but with cached bytecode
            // In production, you might need a custom wrapper
            return new SPIRVUtils.SPIRV(0, cached);
        }

        // Compile using VulkanMod's SPIRVUtils
        LOGGER.info("Compiling shader: {} ({})", shaderName, kind);

        long startTime = System.currentTimeMillis();
        SPIRVUtils.SPIRV spirv;

        try {
            spirv = SPIRVUtils.compileShader(shaderName, source, kind);
        } catch (Exception e) {
            LOGGER.error("Failed to compile shader: {}", shaderName, e);
            throw new RuntimeException("Shader compilation failed: " + shaderName, e);
        }

        long duration = System.currentTimeMillis() - startTime;
        LOGGER.info("Compiled {} in {}ms", shaderName, duration);

        // Cache the result
        cache.put(cacheKey, spirv.bytecode());

        return spirv;
    }

    /**
     * Generate a unique cache key for a shader
     */
    private String generateCacheKey(String name, String source,
                                    SPIRVUtils.ShaderKind kind,
                                    String version) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Hash: shader name + source + kind + version
            digest.update(name.getBytes());
            digest.update(source.getBytes());
            digest.update(kind.name().getBytes());
            digest.update(version.getBytes());

            byte[] hash = digest.digest();

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
