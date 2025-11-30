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
     * Initialize custom pipelines using this as a template
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onGraphicsPipelineCreate(CallbackInfo ci) {
        if (!customPipelinesReady && PipelineManager.areOverridesEnabled()) {
            LOGGER.info("✓ VulkanMod GraphicsPipeline system is ready");
            customPipelinesReady = true;
        }

        // Get the pipeline name
        String pipelineName = ((PipelineAccessor) this).getName();
        GraphicsPipeline thisPipeline = (GraphicsPipeline) (Object) this;

        // Check if we have a custom pipeline for this name
        PipelineManager.getPipeline(pipelineName).ifPresent(customPipeline -> {
            if (!customPipeline.isInitialized()) {
                LOGGER.info("Initializing custom pipeline '{}' from VulkanMod template", pipelineName);
                PipelineManager.initializePipeline(pipelineName, thisPipeline);

                if (customPipeline.isInitialized()) {
                    LOGGER.info("✓ Custom shader '{}' from {} is now active!",
                            pipelineName,
                            customPipeline.getSourcePack().getName());
                }
            }
        });
    }
}
