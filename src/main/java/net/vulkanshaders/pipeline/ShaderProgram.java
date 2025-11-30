package net.vulkanshaders.pipeline;

import java.util.Properties;

/**
 * Represents a shader program (vertex + fragment pair) from a shader pack
 */
public class ShaderProgram {
    private final String name;
    private final String vertexSource;
    private final String fragmentSource;
    private final Properties properties;
    private final boolean isOverride;
    private final String vanillaTarget;

    public ShaderProgram(String name, String vertexSource, String fragmentSource,
                         Properties properties, boolean isOverride, String vanillaTarget) {
        this.name = name;
        this.vertexSource = vertexSource;
        this.fragmentSource = fragmentSource;
        this.properties = properties;
        this.isOverride = isOverride;
        this.vanillaTarget = vanillaTarget;
    }

    public String getName() { return name; }
    public String getVertexSource() { return vertexSource; }
    public String getFragmentSource() { return fragmentSource; }
    public Properties getProperties() { return properties; }
    public boolean isOverride() { return isOverride; }
    public String getVanillaTarget() { return vanillaTarget; }
}
