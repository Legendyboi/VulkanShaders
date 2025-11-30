package net.vulkanshaders.mixin;

import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanshaders.pipeline.PipelineManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GraphicsPipeline.class, remap = false)
public class GraphicsPipelineMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("VulkanShaders/Mixin");
    private static boolean customPipelinesReady = false;

    /**
     * Inject at the end of the GraphicsPipeline constructor
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onGraphicsPipelineCreate(CallbackInfo ci) {
        // Just log that VulkanMod's pipelines are being created
        if (!customPipelinesReady && PipelineManager.areOverridesEnabled()) {
            LOGGER.info("✓ VulkanMod GraphicsPipeline system is ready");
            LOGGER.info("Custom pipelines loaded and ready (not yet applied)");
            customPipelinesReady = true;
        }

        // Get the pipeline name using the accessor
        String pipelineName = ((PipelineAccessor) this).getName();

        // Check if this pipeline has a custom override
        PipelineManager.getOverride(pipelineName).ifPresent(customPipeline -> {
            LOGGER.info("Pipeline '{}' has a custom shader available: {} from {}",
                    pipelineName,
                    customPipeline.getName(),
                    customPipeline.getSourcePack().getName());
        });
    }
}
