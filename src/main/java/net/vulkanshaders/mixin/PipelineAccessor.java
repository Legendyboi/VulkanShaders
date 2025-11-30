package net.vulkanshaders.mixin;

import net.vulkanmod.vulkan.shader.Pipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Pipeline.class, remap = false)
public interface PipelineAccessor {
    @Accessor("name")
    String getName();
}
