package net.vulkanshaders.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class PackMetadata {

    public String name;
    public String version;
    public String author;
    public String description;

    @SerializedName("minecraft_version")
    public String minecraftVersion;

    @SerializedName("vulkanmod_version")
    public String vulkanmodVersion;

    public Map<String, PipelineConfig> pipelines;
    public UniformsConfig uniform;
    public Map<String, OptionConfig> options;

    public static class UniformsConfig {
        public List<CustomUniform> custom;
    }

    public static class CustomUniform {
        public String name;
        public String type;
        public String source;
    }

    public static class OptionConfig {
        public String type;
        public Object defaultValue;
        public String description;
        public Double min;
        public Double max;
    }


}
