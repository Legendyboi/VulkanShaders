package net.vulkanshaders.mixin;

import net.vulkanmod.vulkan.shader.Pipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.nio.ByteBuffer;

@Mixin(value = Pipeline.class, remap = false)
public interface PipelineAccessorMethods {

    @Invoker("createShaderModule")
    static long invokeCreateShaderModule(ByteBuffer spirvCode) {
        throw new AssertionError();
    }
}
