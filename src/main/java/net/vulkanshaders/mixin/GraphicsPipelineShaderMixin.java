package net.vulkanshaders.mixin;

import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.SPIRVUtils;
import net.vulkanshaders.pipeline.PipelineManager;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(value = GraphicsPipeline.class, remap = false)
public class GraphicsPipelineShaderMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("VulkanShaders/ShaderMixin");

    @Shadow
    private long vertShaderModule;

    @Shadow
    private long fragShaderModule;

    /**
     * Inject into createShaderModules to replace with custom shaders
     */
    @Inject(method = "createShaderModules", at = @At("HEAD"), cancellable = true)
    private void onCreateShaderModules(SPIRVUtils.SPIRV vertSpirv, SPIRVUtils.SPIRV fragSpirv, CallbackInfo ci) {
        // Get the pipeline name
        String pipelineName = ((PipelineAccessor) this).getName();

        // Check if we have a custom override
        PipelineManager.getPipeline(pipelineName).ifPresent(customPipeline -> {
            LOGGER.info("🔵 Replacing shaders for pipeline: {}", pipelineName);

            // Use custom shaders instead
            SPIRVUtils.SPIRV customVert = customPipeline.getVertexShader();
            SPIRVUtils.SPIRV customFrag = customPipeline.getFragmentShader();

            // Get bytecode
            ByteBuffer vertBuffer = customVert.bytecode();
            ByteBuffer fragBuffer = customFrag.bytecode();

            // Copy to DIRECT ByteBuffers (required by Vulkan native calls)
            ByteBuffer directVertBuffer = copyToDirectBuffer(vertBuffer);
            ByteBuffer directFragBuffer = copyToDirectBuffer(fragBuffer);

            try {
                // Create shader modules using direct buffers
                LOGGER.info("Creating vertex shader module...");
                this.vertShaderModule = PipelineAccessorMethods.invokeCreateShaderModule(directVertBuffer);
                LOGGER.info("✓ Vertex shader module created");

                LOGGER.info("Creating fragment shader module...");
                this.fragShaderModule = PipelineAccessorMethods.invokeCreateShaderModule(directFragBuffer);
                LOGGER.info("✓ Fragment shader module created");

                LOGGER.info("✓ Custom shaders ACTIVE for: {}", pipelineName);
            } finally {
                // Free the direct buffers
                MemoryUtil.memFree(directVertBuffer);
                MemoryUtil.memFree(directFragBuffer);
            }

            // Cancel the original method
            ci.cancel();
        });
    }

    /**
     * Copy a ByteBuffer to a direct ByteBuffer
     */
    private static ByteBuffer copyToDirectBuffer(ByteBuffer source) {
        // Create a direct buffer
        ByteBuffer direct = MemoryUtil.memAlloc(source.capacity());

        // Copy data
        source.rewind();
        direct.put(source);
        direct.rewind();

        return direct;
    }
}
