package net.vulkanshaders.pipeline;

public class PipelineOverride {
    private final CustomPipeline customPipeline;
    private final boolean enabled;

    public PipelineOverride(CustomPipeline customPipeline) {
        this(customPipeline, true);
    }

    public PipelineOverride(CustomPipeline customPipeline, boolean enabled) {
        this.customPipeline = customPipeline;
        this.enabled = enabled;
    }

    public CustomPipeline getCustomPipeline() {
        return customPipeline;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
