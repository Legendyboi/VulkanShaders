package net.vulkanshaders.model;

import java.nio.file.Path;
import java.util.Map;

public class ShaderPack {

    private final PackMetadata metadata;
    private final Map<String, String> shaderSources;
    private final Path packPath;
    private boolean enabled;

    public ShaderPack(PackMetadata metadata, Map<String, String> shaderSources, Path packPath) {
        this.metadata = metadata;
        this.shaderSources = shaderSources;
        this.packPath = packPath;
        this.enabled = true;
    }

    public String getName() {
        return metadata.name;
    }

    public String getVersion() {
        return metadata.version;
    }

    public PackMetadata getMetadata() {
        return metadata;
    }

    public String getShaderSource(String path) {
        return shaderSources.get(path);
    }

    public Map<String, String> getAllShaderSources() {
        return shaderSources;
    }

    public Path getPackPath() {
        return packPath;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}